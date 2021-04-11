package com.github.mrbean355.zakbot

import net.dean.jraw.RedditClient
import net.dean.jraw.models.Comment
import org.springframework.stereotype.Service

private const val PostFormat = "%s\n\n^(I'm a) [^(bot)]($GitHubUrl)^(! Please report issues to) [^(/u/$AuthorUsername)](https://www.reddit.com/user/$AuthorUsername)^(.)"

@Service
class RedditService(private val client: RedditClient) {

    fun getLatestComments(): List<Comment> =
        client.latestComments(SubredditName)
            .build()
            .accumulateMerged(1)

    fun replyToComment(comment: Comment, response: String) {
        client.comment(comment.id).reply(PostFormat.format(response))
    }
}