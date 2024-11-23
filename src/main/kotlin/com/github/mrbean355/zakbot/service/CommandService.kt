package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.BotUsername
import com.github.mrbean355.zakbot.db.BotCache
import com.github.mrbean355.zakbot.util.getString
import net.dean.jraw.models.Comment
import org.springframework.stereotype.Service

private const val COMMAND_PREFIX = "!$BotUsername "

@Service
class CommandService(
    private val redditService: RedditService,
    private val botCache: BotCache,
) {

    fun processAuthorCommand(comment: Comment) {
        if (!comment.body.startsWith(COMMAND_PREFIX)) {
            return
        }

        val command = comment.body.substringAfter(COMMAND_PREFIX).trim()
        val name = command.substringBefore(' ')
        val args = command.substringAfter(name).trim()

        when (name) {
            "ignore\\_user" -> ignoreUser(args, comment)
            "unignore\\_user" -> unignoreUser(args, comment)
            "ignore\\_post" -> ignoreSubmission(args, comment)
            "unignore\\_post" -> unignoreSubmission(comment)
        }
    }

    private fun ignoreUser(id: String, comment: Comment) {
        if (botCache.isUserIgnored(id)) {
            redditService.replyToComment(comment, getString("reddit.command.ignoreUser.alreadyIgnored", id))
            return
        }
        if (redditService.userExists(id)) {
            botCache.ignoreUser(id, comment.fullName)
            redditService.replyToComment(comment, getString("reddit.command.ignoreUser.success", id))
        } else {
            redditService.replyToComment(comment, getString("reddit.command.ignoreUser.notFound", id))
        }
    }

    private fun unignoreUser(id: String, comment: Comment) {
        if (!botCache.isUserIgnored(id)) {
            redditService.replyToComment(comment, getString("reddit.command.unignoreUser.notIgnored", id))
            return
        }
        botCache.unignoreUser(id)
        redditService.replyToComment(comment, getString("reddit.command.unignoreUser.success", id))
    }

    private fun ignoreSubmission(reason: String, comment: Comment) {
        if (botCache.isSubmissionIgnored(comment.submissionFullName)) {
            redditService.replyToComment(comment, getString("reddit.command.ignoreSubmission.alreadyIgnored"))
            return
        }
        botCache.ignoreSubmission(comment.submissionFullName, reason.ifBlank { null })
        redditService.replyToComment(comment, getString("reddit.command.ignoreSubmission.success"))
    }

    private fun unignoreSubmission(comment: Comment) {
        if (!botCache.isSubmissionIgnored(comment.submissionFullName)) {
            redditService.replyToComment(comment, getString("reddit.command.unignoreSubmission.notIgnored"))
            return
        }
        botCache.unignoreSubmission(comment.submissionFullName)
        redditService.replyToComment(comment, getString("reddit.command.unignoreSubmission.success"))
    }
}
