package com.github.mrbean355.zakbot

import net.dean.jraw.RedditClient
import net.dean.jraw.models.Comment
import net.dean.jraw.models.PublicContribution
import net.dean.jraw.models.Submission
import net.dean.jraw.models.SubredditSort
import net.dean.jraw.pagination.Paginator
import org.springframework.stereotype.Service
import java.util.Date

/** Number of posts/comments per page. */
private const val PageSizeLimit = 5

/** Max number of pages to look back through that haven't been checked before. */
private const val PageHistoryLimit = 10

@Service
class RedditService(private val client: RedditClient) {

    /** Get all submissions (posts) created after the given date. */
    fun getSubmissionsSince(date: Date): List<Submission> =
        client.subreddit(SubredditName).posts()
            .sorting(SubredditSort.NEW)
            .limit(PageSizeLimit)
            .build()
            .collectAfter(date)

    /** Get all comments created after the given date. */
    fun getCommentsSince(date: Date): List<Comment> =
        client.latestComments(SubredditName)
            .limit(PageSizeLimit)
            .build()
            .collectAfter(date)

    fun replyToSubmission(submission: Submission, response: String) {
        client.submission(submission.id).reply(response)
    }

    fun replyToComment(comment: Comment, response: String) {
        client.comment(comment.id).reply(response)
    }

    /** @return the submission that the [comment] belongs to, or null if not found. */
    fun getCommentSubmission(comment: Comment): Submission? {
        return client.lookup(comment.submissionFullName).firstOrNull() as? Submission
    }

    /** @return the parent comment of the [comment], or null if the parent is not a comment. */
    fun findParentComment(comment: Comment): Comment? {
        return client.lookup(comment.parentFullName).firstOrNull() as? Comment
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
            if (++pages >= PageHistoryLimit) {
                return items
            }
        }

        return items
    }
}