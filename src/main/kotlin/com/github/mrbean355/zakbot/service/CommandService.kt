package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.BotUsername
import com.github.mrbean355.zakbot.TelegramNotifier
import com.github.mrbean355.zakbot.db.BotCache
import com.github.mrbean355.zakbot.util.asPlainText
import com.github.mrbean355.zakbot.util.getString
import net.dean.jraw.models.Comment
import org.springframework.stereotype.Service

private const val COMMAND_PREFIX = "!$BotUsername "

@Service
class CommandService(
    private val redditService: RedditService,
    private val botCache: BotCache,
    private val telegramNotifier: TelegramNotifier,
) {

    fun processAuthorCommand(comment: Comment) {
        val body = comment.body.asPlainText()
        if (!body.startsWith(COMMAND_PREFIX)) {
            return
        }

        val command = body.substringAfter(COMMAND_PREFIX).trim()
        val name = command.substringBefore(' ')
        val args = command.substringAfter(name).trim()

        when (name) {
            "ignore_user" -> ignoreUser(args, comment)
            "unignore_user" -> unignoreUser(args, comment)
            "ignore_post" -> ignoreSubmission(args, comment)
            "unignore_post" -> unignoreSubmission(comment)
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
            telegramNotifier.sendMessage(getString("telegram.command_ignore_user", id, comment.url))
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
        telegramNotifier.sendMessage(getString("telegram.command_unignore_user", id, comment.url))
    }

    private fun ignoreSubmission(reason: String, comment: Comment) {
        if (botCache.isSubmissionIgnored(comment.submissionFullName)) {
            redditService.replyToComment(comment, getString("reddit.command.ignoreSubmission.alreadyIgnored"))
            return
        }
        botCache.ignoreSubmission(comment.submissionFullName, reason.ifBlank { null })
        redditService.replyToComment(comment, getString("reddit.command.ignoreSubmission.success"))
        telegramNotifier.sendMessage(getString("telegram.command_ignore_post", reason.ifBlank { "None" }, comment.url))
    }

    private fun unignoreSubmission(comment: Comment) {
        if (!botCache.isSubmissionIgnored(comment.submissionFullName)) {
            redditService.replyToComment(comment, getString("reddit.command.unignoreSubmission.notIgnored"))
            return
        }
        botCache.unignoreSubmission(comment.submissionFullName)
        redditService.replyToComment(comment, getString("reddit.command.unignoreSubmission.success"))
        telegramNotifier.sendMessage(getString("telegram.command_unignore_post", comment.url))
    }
}
