package com.github.mrbean355.zakbot.reddit

import com.github.mrbean355.zakbot.BotUsername
import com.github.mrbean355.zakbot.SubredditName
import com.github.mrbean355.zakbot.reddit.dto.RedditCommentDto
import com.github.mrbean355.zakbot.reddit.dto.RedditCommentReplyResponse
import com.github.mrbean355.zakbot.reddit.dto.RedditFlairDto
import com.github.mrbean355.zakbot.reddit.dto.RedditListing
import com.github.mrbean355.zakbot.reddit.dto.RedditSubmissionDto
import com.github.mrbean355.zakbot.reddit.dto.RedditUserAboutDto
import com.github.mrbean355.zakbot.reddit.model.Comment
import com.github.mrbean355.zakbot.reddit.model.Contribution
import com.github.mrbean355.zakbot.reddit.model.RedditFlair
import com.github.mrbean355.zakbot.reddit.model.Submission
import com.github.mrbean355.zakbot.config.ZakbotProperties
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import java.time.Instant

/** Number of posts/comments per page. */
private const val PageSizeLimit = 25

/** Max number of pages to look back through that haven't been checked before. */
private const val PageHistoryLimit = 10

/** Max number of fullnames Reddit's /api/info endpoint accepts per request. */
private const val InfoBatchLimit = 100

@Service
class RedditService(
    private val client: RestClient,
    private val zakbotProperties: ZakbotProperties,
) {
    private val logger = LoggerFactory.getLogger(RedditService::class.java)

    /** Get all submissions (posts) created after the given instant. */
    fun getSubmissionsSince(since: Instant): List<Submission> =
        fetchContributionsSince(
            path = "/r/{subreddit}/new",
            typeRef = object : ParameterizedTypeReference<RedditListing<RedditSubmissionDto>>() {},
            since = since,
            transform = { it.toDomain() },
        ).onEach { submissionCache[it.fullName] = it }

    /** Get all comments created after the given instant. */
    fun getCommentsSince(since: Instant): List<Comment> =
        fetchContributionsSince(
            path = "/r/{subreddit}/comments",
            typeRef = object : ParameterizedTypeReference<RedditListing<RedditCommentDto>>() {},
            since = since,
            transform = { it.toDomain() },
        ).onEach { commentCache[it.fullName] = it }

    private inline fun <DTO, DOMAIN : Contribution> fetchContributionsSince(
        path: String,
        typeRef: ParameterizedTypeReference<RedditListing<DTO>>,
        since: Instant,
        crossinline transform: (DTO) -> DOMAIN,
    ): List<DOMAIN> {
        val items = mutableListOf<DOMAIN>()
        var after: String? = null
        var pages = 0

        while (pages < PageHistoryLimit) {
            val currentAfter = after
            val response = client.get()
                .uri { builder ->
                    builder.path(path)
                        .queryParam("limit", PageSizeLimit)
                        .apply { if (currentAfter != null) queryParam("after", currentAfter) }
                        .build(SubredditName)
                }
                .retrieve()
                .body(typeRef)

            val children = response?.data?.children.orEmpty()
            if (children.isEmpty()) break

            for (child in children) {
                val item = transform(child.data)
                if (item.created > since) {
                    items += item
                } else {
                    return items
                }
            }

            after = response?.data?.after
            if (after == null) break
            pages++
        }

        return items
    }

    fun replyToSubmission(submission: Submission, response: String): String? {
        return if (zakbotProperties.replies.enabled) {
            postCommentReply(submission.fullName, response)
        } else {
            logger.info("Reply to submission '{}': {}", submission.title, response)
            null
        }
    }

    fun replyToComment(comment: Comment, response: String): String? {
        return if (zakbotProperties.replies.enabled) {
            postCommentReply(comment.fullName, response)
        } else {
            logger.info("Reply to comment '{}': {}", comment.body, response)
            null
        }
    }

    private val submissionCache = lruCache<Submission>(100)
    private val commentCache = lruCache<Comment>(200)

    private fun <V> lruCache(maxSize: Int): MutableMap<String, V> =
        java.util.Collections.synchronizedMap(
            object : java.util.LinkedHashMap<String, V>(64, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, V>?) = size > maxSize
            }
        )

    private inline fun <DTO, DOMAIN : Any> prefetchBatch(
        ids: List<String>,
        typeRef: ParameterizedTypeReference<RedditListing<DTO>>,
        cache: MutableMap<String, DOMAIN>,
        crossinline keySelector: (DOMAIN) -> String,
        crossinline transform: (DTO) -> DOMAIN,
    ) {
        if (ids.isEmpty()) return

        ids.chunked(InfoBatchLimit).forEach { batch ->
            try {
                val response = client.get()
                    .uri("/api/info?id=${batch.joinToString(",")}")
                    .retrieve()
                    .body(typeRef)

                response?.data?.children.orEmpty().forEach { child ->
                    val domain = transform(child.data)
                    cache[keySelector(domain)] = domain
                }
            } catch (ex: Exception) {
                logger.warn("Failed to prefetch info for {} item(s)", batch.size, ex)
            }
        }
    }

    /** Prefetch uncached submissions and parent comments for [comments] in batches. */
    fun prefetchInfo(comments: List<Comment>) {
        val missingSubmissions = comments.asSequence()
            .map { it.submissionFullName }
            .filter { it.startsWith("t3_") && it !in submissionCache }
            .distinct()
            .toList()
        prefetchBatch(
            ids = missingSubmissions,
            typeRef = object : ParameterizedTypeReference<RedditListing<RedditSubmissionDto>>() {},
            cache = submissionCache,
            keySelector = { it.fullName },
            transform = { it.toDomain() },
        )

        val missingComments = comments.asSequence()
            .map { it.parentFullName }
            .filter { it.startsWith("t1_") && it !in commentCache }
            .distinct()
            .toList()
        prefetchBatch(
            ids = missingComments,
            typeRef = object : ParameterizedTypeReference<RedditListing<RedditCommentDto>>() {},
            cache = commentCache,
            keySelector = { it.fullName },
            transform = { it.toDomain() },
        )
    }

    /** @return the submission that the [comment] belongs to, or null if not found. */
    fun getCommentSubmission(comment: Comment): Submission? {
        submissionCache[comment.submissionFullName]?.let { return it }

        return try {
            val response = client.get()
                .uri("/api/info?id={id}", comment.submissionFullName)
                .retrieve()
                .body(object : ParameterizedTypeReference<RedditListing<RedditSubmissionDto>>() {})

            response?.data?.children?.firstOrNull()?.data?.toDomain()?.also {
                submissionCache[comment.submissionFullName] = it
            }
        } catch (ex: Exception) {
            logger.warn("Failed to get submission for comment: {}", comment.fullName, ex)
            null
        }
    }

    /** @return the parent comment of the [comment], or null if the parent is not a comment. */
    fun findParentComment(comment: Comment): Comment? {
        if (!comment.parentFullName.startsWith("t1_")) {
            return null
        }
        commentCache[comment.parentFullName]?.let { return it }

        return try {
            val response = client.get()
                .uri("/api/info?id={id}", comment.parentFullName)
                .retrieve()
                .body(object : ParameterizedTypeReference<RedditListing<RedditCommentDto>>() {})

            response?.data?.children?.firstOrNull()?.data?.toDomain()?.also {
                commentCache[comment.parentFullName] = it
            }
        } catch (ex: Exception) {
            logger.warn("Failed to find parent comment: {}", comment.parentFullName, ex)
            null
        }
    }

    fun userExists(name: String): Boolean {
        return try {
            val response = client.get()
                .uri("/user/{name}/about", name)
                .retrieve()
                .body(RedditUserAboutDto::class.java)

            response?.data?.let { it.isSuspended != true } ?: false
        } catch (ex: HttpClientErrorException.NotFound) {
            false
        } catch (ex: Exception) {
            logger.warn("Failed to check if user exists: {}", name, ex)
            false
        }
    }

    fun getFlairOptions(): List<RedditFlair> {
        return try {
            val response = client.get()
                .uri("/r/{subreddit}/api/user_flair_v2", SubredditName)
                .retrieve()
                .body(object : ParameterizedTypeReference<List<RedditFlairDto>>() {})

            response.orEmpty().map { RedditFlair(it.id, it.text) }
        } catch (ex: Exception) {
            logger.warn("Failed to get flair options", ex)
            emptyList()
        }
    }

    fun setBotFlair(flair: RedditFlair) {
        if (!zakbotProperties.replies.enabled) {
            logger.info("Set bot flair to: {}", flair.text)
            return
        }
        try {
            val body = LinkedMultiValueMap<String, String>().apply {
                add("api_type", "json")
                add("name", BotUsername)
                add("flair_template_id", flair.id)
                add("text", flair.text)
            }

            client.post()
                .uri("/r/{subreddit}/api/selectflair", SubredditName)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .toBodilessEntity()
        } catch (ex: Exception) {
            logger.error("Failed to set bot flair to: {}", flair.text, ex)
        }
    }

    /** Get the bot's own recent comments to check for downvotes. */
    fun getOwnRecentComments(limit: Int = 100): List<Comment> {
        return try {
            val response = client.get()
                .uri { builder ->
                    builder.path("/user/{username}/comments")
                        .queryParam("limit", limit)
                        .queryParam("sort", "new")
                        .build(BotUsername)
                }
                .retrieve()
                .body(object : ParameterizedTypeReference<RedditListing<RedditCommentDto>>() {})

            response?.data?.children?.map { it.data.toDomain() }.orEmpty()
                .onEach { commentCache[it.fullName] = it }
        } catch (ex: Exception) {
            logger.warn("Failed to fetch bot comments for downvote check", ex)
            emptyList()
        }
    }

    fun deleteComment(comment: Comment): Boolean {
        return if (zakbotProperties.replies.enabled) {
            performDeleteComment(comment.fullName)
        } else {
            logger.info("Delete downvoted comment '{}' (score: {})", comment.fullName, comment.score)
            true
        }
    }

    fun deleteCommentById(thingId: String): Boolean {
        return if (zakbotProperties.replies.enabled) {
            performDeleteComment(thingId)
        } else {
            logger.info("Delete comment '{}'", thingId)
            true
        }
    }

    private fun performDeleteComment(thingId: String): Boolean {
        return try {
            val body = LinkedMultiValueMap<String, String>().apply {
                add("api_type", "json")
                add("id", thingId)
            }

            client.post()
                .uri("/api/del")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .toBodilessEntity()
            true
        } catch (ex: Exception) {
            logger.error("Failed to delete comment: {}", thingId, ex)
            false
        }
    }

    private fun postCommentReply(thingId: String, text: String): String? {
        return try {
            val body = LinkedMultiValueMap<String, String>().apply {
                add("api_type", "json")
                add("thing_id", thingId)
                add("text", text)
            }

            val response = client.post()
                .uri("/api/comment")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(RedditCommentReplyResponse::class.java)
            response?.json?.data?.things?.firstOrNull()?.data?.name
        } catch (ex: Exception) {
            logger.error("Failed to post comment reply to thing: {}", thingId, ex)
            null
        }
    }

    private fun RedditSubmissionDto.toDomain() = Submission(
        id = id,
        fullName = name,
        author = author,
        created = Instant.ofEpochMilli((createdUtc * 1000).toLong()),
        url = "https://www.reddit.com$permalink",
        title = title,
        selfText = selfText,
    )

    private fun RedditCommentDto.toDomain() = Comment(
        id = id,
        fullName = name,
        author = author,
        created = Instant.ofEpochMilli((createdUtc * 1000).toLong()),
        url = "https://www.reddit.com$permalink",
        body = body,
        submissionFullName = linkId,
        parentFullName = parentId,
        score = score ?: 0,
    )
}