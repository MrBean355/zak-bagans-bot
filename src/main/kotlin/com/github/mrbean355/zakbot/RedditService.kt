package com.github.mrbean355.zakbot

import net.dean.jraw.RedditClient
import net.dean.jraw.models.Comment
import net.dean.jraw.models.PublicContribution
import net.dean.jraw.models.Submission
import net.dean.jraw.models.SubredditSort
import net.dean.jraw.models.UserHistorySort
import net.dean.jraw.pagination.DefaultPaginator
import net.dean.jraw.pagination.Paginator
import org.springframework.stereotype.Service
import java.util.Date

private const val PageLimit = 10

@Service
class RedditService(private val client: RedditClient) {

    /** Get all submissions (posts) created after the given date. */
    fun getSubmissionsSince(date: Date): List<Submission> =
        client.subreddit(SubredditName).posts()
            .sorting(SubredditSort.NEW)
            .build()
            .collectAfter(date)

    /** Get all comments created after the given date. */
    fun getCommentsSince(date: Date): List<Comment> =
        client.latestComments(SubredditName)
            .build()
            .collectAfter(date)

    fun replyToSubmission(submission: Submission, response: String) {
        client.submission(submission.id).reply(response)
    }

    fun replyToComment(comment: Comment, response: String) {
        client.comment(comment.id).reply(response)
    }

    /** @return the parent comment of [comment], or null if the parent is not a comment. */
    fun findParentComment(comment: Comment): Comment? {
        return client.lookup(comment.parentFullName).firstOrNull() as? Comment
    }

    fun getAllBotComments(): DefaultPaginator<PublicContribution<*>> {
        return client.me()
            .history("comments")
            .sorting(UserHistorySort.NEW)
            .limit(Paginator.RECOMMENDED_MAX_LIMIT)
            .build()
    }

    /** Collects all contributions that have been created after the given date. */
    private fun <T : PublicContribution<*>> Paginator<T>.collectAfter(since: Date): List<T> {
        val items = mutableListOf<T>()
        var pages = 0

        forEach { listing ->
            listing.forEach { contribution ->
                if (contribution.created > since) {
                    items += contribution
                } else {
                    return items
                }
            }
            if (++pages >= PageLimit) {
                return items
            }
        }

        return items
    }
}