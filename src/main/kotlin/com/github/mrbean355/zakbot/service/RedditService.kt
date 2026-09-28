package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.BotUsername
import com.github.mrbean355.zakbot.SubredditName
import com.github.mrbean355.zakbot.reddit.dto.RedditCommentDto
import com.github.mrbean355.zakbot.reddit.dto.RedditFlairDto
import com.github.mrbean355.zakbot.reddit.dto.RedditListing
import com.github.mrbean355.zakbot.reddit.dto.RedditSubmissionDto
import com.github.mrbean355.zakbot.reddit.dto.RedditUserAboutDto
import com.github.mrbean355.zakbot.reddit.model.Comment
import com.github.mrbean355.zakbot.reddit.model.Contribution
import com.github.mrbean355.zakbot.reddit.model.RedditFlair
import com.github.mrbean355.zakbot.reddit.model.Submission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import java.time.Instant

/** Number of posts/comments per page. */
private const val PageSizeLimit = 5

/** Max number of pages to look back through that haven't been checked before. */
private const val PageHistoryLimit = 10

@Service
class RedditService(
    private val client: RestClient,
    @Value($$"${zakbot.replies.enabled:false}") private val sendReplies: Boolean,
) {
    private val logger = LoggerFactory.getLogger(RedditService::class.java)

    /** Get all submissions (posts) created after the given instant. */
    fun getSubmissionsSince(since: Instant): List<Submission> =
        fetchContributionsSince(
            path = "/r/{subreddit}/new",
            typeRef = object : ParameterizedTypeReference<RedditListing<RedditSubmissionDto>>() {},
            since = since,
            transform = { it.toDomain() },
        )

    /** Get all comments created after the given instant. */
    fun getCommentsSince(since: Instant): List<Comment> =
        fetchContributionsSince(
            path = "/r/{subreddit}/comments",
            typeRef = object : ParameterizedTypeReference<RedditListing<RedditCommentDto>>() {},
            since = since,
            transform = { it.toDomain() },
        )

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

    fun replyToSubmission(submission: Submission, response: String) {
        if (sendReplies) {
            postCommentReply(submission.fullName, response)
        } else {
            logger.info("Reply to submission '{}': {}", submission.title, response)
        }
    }

    fun replyToComment(comment: Comment, response: String) {
        if (sendReplies) {
            postCommentReply(comment.fullName, response)
        } else {
            logger.info("Reply to comment '{}': {}", comment.body, response)
        }
    }

    /** @return the submission that the [comment] belongs to, or null if not found. */
    fun getCommentSubmission(comment: Comment): Submission? {
        val response = client.get()
            .uri("/api/info?id={id}", comment.submissionFullName)
            .retrieve()
            .body(object : ParameterizedTypeReference<RedditListing<RedditSubmissionDto>>() {})

        return response?.data?.children?.firstOrNull()?.data?.toDomain()
    }

    /** @return the parent comment of the [comment], or null if the parent is not a comment. */
    fun findParentComment(comment: Comment): Comment? {
        if (!comment.parentFullName.startsWith("t1_")) {
            return null
        }
        val response = client.get()
            .uri("/api/info?id={id}", comment.parentFullName)
            .retrieve()
            .body(object : ParameterizedTypeReference<RedditListing<RedditCommentDto>>() {})

        return response?.data?.children?.firstOrNull()?.data?.toDomain()
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
        val response = client.get()
            .uri("/r/{subreddit}/api/user_flair_v2", SubredditName)
            .retrieve()
            .body(object : ParameterizedTypeReference<List<RedditFlairDto>>() {})

        return response.orEmpty().map { RedditFlair(it.id, it.text) }
    }

    fun setBotFlair(flair: RedditFlair) {
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
    }

    private fun postCommentReply(thingId: String, text: String) {
        val body = LinkedMultiValueMap<String, String>().apply {
            add("api_type", "json")
            add("thing_id", thingId)
            add("text", text)
        }

        client.post()
            .uri("/api/comment")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .toBodilessEntity()
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
    )
}