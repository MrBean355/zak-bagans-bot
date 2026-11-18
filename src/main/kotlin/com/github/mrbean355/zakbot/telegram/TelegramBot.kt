package com.github.mrbean355.zakbot.telegram

import com.github.mrbean355.zakbot.SubredditName
import com.github.mrbean355.zakbot.config.GeminiProperties
import com.github.mrbean355.zakbot.config.TelegramProperties
import com.github.mrbean355.zakbot.service.BotStateService
import com.github.mrbean355.zakbot.db.repo.IgnoredSubmissionRepository
import com.github.mrbean355.zakbot.db.repo.IgnoredUserRepository
import com.github.mrbean355.zakbot.db.repo.PhraseRepository
import com.github.mrbean355.zakbot.reddit.RedditService
import com.github.mrbean355.zakbot.util.getString
import org.slf4j.LoggerFactory
import org.springframework.boot.info.BuildProperties
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.meta.generics.TelegramClient
import java.time.Duration
import java.time.Instant

private const val PREFIX_IGNORE_USER = "iu:"
private const val PREFIX_UNIGNORE_USER = "uu:"
private const val PREFIX_IGNORE_POST = "ip:"
private const val PREFIX_UNIGNORE_POST = "up:"
private const val PREFIX_DELETE_COMMENT = "dc:"
private const val CALLBACK_COMMENT_DELETED = "cd"
private const val CALLBACK_REFRESH_STATUS = "rs"

@Component
class TelegramBot(
    private val applicationContext: ApplicationContext,
    private val phraseRepository: PhraseRepository,
    private val ignoredUserRepository: IgnoredUserRepository,
    private val ignoredSubmissionRepository: IgnoredSubmissionRepository,
    private val botStateService: BotStateService,
    private val redditService: RedditService,
    private val telegramClient: TelegramClient,
    private val buildProperties: BuildProperties,
    private val telegramProperties: TelegramProperties,
    private val geminiProperties: GeminiProperties,
) : SpringLongPollingBot, LongPollingUpdateConsumer, TelegramNotifier {

    private val logger = LoggerFactory.getLogger(TelegramBot::class.java)

    override fun getBotToken(): String = telegramProperties.token

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer = this

    override fun consume(updates: List<Update>) {
        for (update in updates) {
            update.callbackQuery?.let { callbackQuery ->
                handleCallbackQuery(callbackQuery)
            }
            update.message?.let { message ->
                if (message.chatId.toString() == telegramProperties.chatId) {
                    val text = message.text.orEmpty().trim().lowercase()
                    if (text == "/status" || text.startsWith("/status@")) {
                        sendMessage(buildStatusMessage(), listOf(TelegramAction.RefreshStatus))
                    }
                }
            }
        }
    }

    override fun sendMessage(text: String, actions: List<TelegramAction>) {
        val keyboard = buildKeyboard(actions)
        try {
            executeSendMessage(text, keyboard, ParseMode.MARKDOWN)
        } catch (ex: TelegramApiRequestException) {
            if (ex.errorCode == 400) {
                logger.warn("Telegram Markdown failed, retrying as plain text", ex)
                try {
                    executeSendMessage(text, keyboard, parseMode = null)
                } catch (retryEx: Exception) {
                    logger.error("Failed to send Telegram message", retryEx)
                }
            } else {
                logger.error("Failed to send Telegram message", ex)
            }
        } catch (ex: Exception) {
            logger.error("Failed to send Telegram message", ex)
        }
    }

    private fun executeSendMessage(
        text: String,
        keyboard: InlineKeyboardMarkup?,
        parseMode: String?,
    ) {
        val builder = SendMessage.builder()
            .chatId(telegramProperties.chatId)
            .text(text)
            .linkPreviewOptions(LinkPreviewOptions.builder().isDisabled(true).build())

        parseMode?.let { builder.parseMode(it) }
        keyboard?.let { builder.replyMarkup(it) }
        telegramClient.execute(builder.build())
    }

    private fun handleCallbackQuery(callbackQuery: CallbackQuery) {
        val message = callbackQuery.message ?: return
        if (message.chatId.toString() != telegramProperties.chatId) {
            return
        }

        val data = callbackQuery.data.orEmpty()
        try {
            when {
                data.startsWith(PREFIX_IGNORE_USER) -> {
                    val username = data.removePrefix(PREFIX_IGNORE_USER)
                    botStateService.ignoreUser(username, "telegram")
                    answerCallback(callbackQuery.id, "Ignored u/$username")
                    replaceButton(message as? Message, data, TelegramAction.UnignoreUser(username).toButton())
                }

                data.startsWith(PREFIX_UNIGNORE_USER) -> {
                    val username = data.removePrefix(PREFIX_UNIGNORE_USER)
                    botStateService.unignoreUser(username)
                    answerCallback(callbackQuery.id, "Unignored u/$username")
                    replaceButton(message as? Message, data, TelegramAction.IgnoreUser(username).toButton())
                }

                data.startsWith(PREFIX_IGNORE_POST) -> {
                    val fullName = data.removePrefix(PREFIX_IGNORE_POST)
                    botStateService.ignoreSubmission(fullName, "Ignored via Telegram")
                    answerCallback(callbackQuery.id, "Post ignored")
                    replaceButton(message as? Message, data, TelegramAction.UnignoreSubmission(fullName).toButton())
                }

                data.startsWith(PREFIX_UNIGNORE_POST) -> {
                    val fullName = data.removePrefix(PREFIX_UNIGNORE_POST)
                    botStateService.unignoreSubmission(fullName)
                    answerCallback(callbackQuery.id, "Post unignored")
                    replaceButton(message as? Message, data, TelegramAction.IgnoreSubmission(fullName).toButton())
                }

                data.startsWith(PREFIX_DELETE_COMMENT) -> {
                    val fullName = data.removePrefix(PREFIX_DELETE_COMMENT)
                    if (redditService.deleteCommentById(fullName)) {
                        answerCallback(callbackQuery.id, "Reply deleted")
                        val deletedButton = InlineKeyboardButton.builder()
                            .text("Deleted")
                            .callbackData(CALLBACK_COMMENT_DELETED)
                            .build()
                        replaceButton(message as? Message, data, deletedButton)
                    } else {
                        answerCallback(callbackQuery.id, "Failed to delete reply")
                    }
                }

                data == CALLBACK_COMMENT_DELETED -> {
                    answerCallback(callbackQuery.id, "Reply already deleted")
                }

                data == CALLBACK_REFRESH_STATUS -> {
                    answerCallback(callbackQuery.id, "Status refreshed")
                    editStatusMessage(message.chatId.toString(), message.messageId)
                }

                else -> {
                    answerCallback(callbackQuery.id, "Unknown action")
                }
            }
        } catch (ex: Exception) {
            logger.error("Failed to handle Telegram callback query: {}", data, ex)
        }
    }

    private fun answerCallback(callbackQueryId: String, text: String) {
        try {
            telegramClient.execute(
                AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId)
                    .text(text)
                    .build()
            )
        } catch (ex: Exception) {
            logger.error("Failed to answer Telegram callback query", ex)
        }
    }

    private fun replaceButton(message: Message?, targetCallbackData: String, replacement: InlineKeyboardButton) {
        val currentMarkup = message?.replyMarkup ?: return
        val updatedRows = currentMarkup.keyboard.map { row ->
            InlineKeyboardRow(
                row.map { button ->
                    if (button.callbackData == targetCallbackData) replacement else button
                }
            )
        }
        try {
            telegramClient.execute(
                EditMessageReplyMarkup.builder()
                    .chatId(message.chatId.toString())
                    .messageId(message.messageId)
                    .replyMarkup(InlineKeyboardMarkup.builder().keyboard(updatedRows).build())
                    .build()
            )
        } catch (ex: Exception) {
            logger.error("Failed to update Telegram message buttons", ex)
        }
    }

    private fun editStatusMessage(chatId: String, messageId: Int) {
        try {
            telegramClient.execute(
                EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(buildStatusMessage())
                    .parseMode(ParseMode.MARKDOWN)
                    .linkPreviewOptions(LinkPreviewOptions.builder().isDisabled(true).build())
                    .replyMarkup(buildKeyboard(listOf(TelegramAction.RefreshStatus)))
                    .build()
            )
        } catch (ex: Exception) {
            logger.error("Failed to refresh Telegram status message", ex)
        }
    }

    private fun buildKeyboard(actions: List<TelegramAction>): InlineKeyboardMarkup? {
        if (actions.isEmpty()) {
            return null
        }
        val (urlActions, callbackActions) = actions.partition { it is TelegramAction.OpenUrl }
        val rows = buildList {
            callbackActions.chunked(2).forEach { chunk ->
                add(InlineKeyboardRow(chunk.map { it.toButton() }))
            }
            if (urlActions.isNotEmpty()) {
                add(InlineKeyboardRow(urlActions.map { it.toButton() }))
            }
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build()
    }

    private fun TelegramAction.toButton(): InlineKeyboardButton = when (this) {
        is TelegramAction.OpenUrl -> InlineKeyboardButton.builder()
            .text("View on Reddit")
            .url(url)
            .build()

        is TelegramAction.IgnoreUser -> InlineKeyboardButton.builder()
            .text("Ignore User")
            .callbackData("$PREFIX_IGNORE_USER$username")
            .build()

        is TelegramAction.UnignoreUser -> InlineKeyboardButton.builder()
            .text("Unignore User")
            .callbackData("$PREFIX_UNIGNORE_USER$username")
            .build()

        is TelegramAction.IgnoreSubmission -> InlineKeyboardButton.builder()
            .text("Ignore Post")
            .callbackData("$PREFIX_IGNORE_POST$fullName")
            .build()

        is TelegramAction.UnignoreSubmission -> InlineKeyboardButton.builder()
            .text("Unignore Post")
            .callbackData("$PREFIX_UNIGNORE_POST$fullName")
            .build()

        is TelegramAction.DeleteComment -> InlineKeyboardButton.builder()
            .text("Delete Reply")
            .callbackData("$PREFIX_DELETE_COMMENT$fullName")
            .build()

        TelegramAction.RefreshStatus -> InlineKeyboardButton.builder()
            .text("Refresh")
            .callbackData(CALLBACK_REFRESH_STATUS)
            .build()
    }

    private fun buildStatusMessage(): String {
        val geminiStatus = when {
            !geminiProperties.enabled -> "Disabled"
            geminiProperties.apiKey.isBlank() -> "No API key"
            geminiProperties.dryRun -> "Dry run"
            else -> "Enabled"
        }
        return getString(
            "telegram.bot_status_response",
            buildProperties.version,
            getUptime().ifBlank { "< 1 second" },
            getMemoryUsage(),
            geminiStatus,
            SubredditName,
            phraseRepository.count(),
            ignoredUserRepository.count(),
            ignoredSubmissionRepository.count(),
        )
    }

    private fun getMemoryUsage(): String {
        val runtime = Runtime.getRuntime()
        val totalMb = runtime.totalMemory() / (1024 * 1024)
        val freeMb = runtime.freeMemory() / (1024 * 1024)
        val usedMb = totalMb - freeMb
        val maxMb = runtime.maxMemory() / (1024 * 1024)
        return "${usedMb}MB / ${maxMb}MB"
    }

    private fun getUptime(): String {
        val elapsed = Duration.between(Instant.ofEpochMilli(applicationContext.startupDate), Instant.now())
        val days = elapsed.toDaysPart()
        val hours = elapsed.toHoursPart()
        val minutes = elapsed.toMinutesPart()
        val seconds = elapsed.toSecondsPart()

        return buildList {
            if (days > 0) add(getString("telegram.bot_uptime_days", days))
            if (hours > 0) add(getString("telegram.bot_uptime_hours", hours))
            if (minutes > 0) add(getString("telegram.bot_uptime_minutes", minutes))
            if (seconds > 0) add(getString("telegram.bot_uptime_seconds", seconds))
        }.joinToString()
    }
}
