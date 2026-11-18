package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.BotUsername
import com.github.mrbean355.zakbot.telegram.TelegramAction
import com.github.mrbean355.zakbot.telegram.TelegramNotifier
import com.github.mrbean355.zakbot.reddit.RedditService
import com.github.mrbean355.zakbot.reddit.model.Comment
import com.github.mrbean355.zakbot.util.asPlainText
import com.github.mrbean355.zakbot.util.escapeTelegramMarkdown
import com.github.mrbean355.zakbot.util.getString
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service

private const val COMMAND_PREFIX = "u/$BotUsername "

@Service
class CommandService(
    private val redditService: RedditService,
    private val botStateService: BotStateService,
    private val telegramNotifier: TelegramNotifier,
    private val messageSource: MessageSource,
) {

    fun processAuthorCommand(comment: Comment) {
        val body = comment.body.asPlainText().removePrefix("/")
        if (!body.startsWith(COMMAND_PREFIX, ignoreCase = true)) {
            return
        }

        val command = body.substring(COMMAND_PREFIX.length).trim()
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
        if (botStateService.isUserIgnored(id)) {
            redditService.replyToComment(comment, messageSource.getString("reddit.command.ignoreUser.alreadyIgnored", id))
            return
        }
        if (redditService.userExists(id)) {
            botStateService.ignoreUser(id, comment.fullName)
            redditService.replyToComment(comment, messageSource.getString("reddit.command.ignoreUser.success", id))
            telegramNotifier.sendMessage(
                messageSource.getString("telegram.command_ignore_user", id),
                listOf(TelegramAction.UnignoreUser(id), TelegramAction.OpenUrl(comment.url)),
            )
        } else {
            redditService.replyToComment(comment, messageSource.getString("reddit.command.ignoreUser.notFound", id))
        }
    }

    private fun unignoreUser(id: String, comment: Comment) {
        if (!botStateService.isUserIgnored(id)) {
            redditService.replyToComment(comment, messageSource.getString("reddit.command.unignoreUser.notIgnored", id))
            return
        }
        botStateService.unignoreUser(id)
        redditService.replyToComment(comment, messageSource.getString("reddit.command.unignoreUser.success", id))
        telegramNotifier.sendMessage(
            messageSource.getString("telegram.command_unignore_user", id),
            listOf(TelegramAction.IgnoreUser(id), TelegramAction.OpenUrl(comment.url)),
        )
    }

    private fun ignoreSubmission(reason: String, comment: Comment) {
        if (botStateService.isSubmissionIgnored(comment.submissionFullName)) {
            redditService.replyToComment(comment, messageSource.getString("reddit.command.ignoreSubmission.alreadyIgnored"))
            return
        }
        botStateService.ignoreSubmission(comment.submissionFullName, reason.ifBlank { null })
        redditService.replyToComment(comment, messageSource.getString("reddit.command.ignoreSubmission.success"))
        telegramNotifier.sendMessage(
            messageSource.getString("telegram.command_ignore_post", reason.ifBlank { "None" }.escapeTelegramMarkdown()),
            listOf(
                TelegramAction.UnignoreSubmission(comment.submissionFullName),
                TelegramAction.OpenUrl(comment.url),
            ),
        )
    }

    private fun unignoreSubmission(comment: Comment) {
        if (!botStateService.isSubmissionIgnored(comment.submissionFullName)) {
            redditService.replyToComment(comment, messageSource.getString("reddit.command.unignoreSubmission.notIgnored"))
            return
        }
        botStateService.unignoreSubmission(comment.submissionFullName)
        redditService.replyToComment(comment, messageSource.getString("reddit.command.unignoreSubmission.success"))
        telegramNotifier.sendMessage(
            messageSource.getString("telegram.command_unignore_post"),
            listOf(
                TelegramAction.IgnoreSubmission(comment.submissionFullName),
                TelegramAction.OpenUrl(comment.url),
            ),
        )
    }
}
