package com.github.mrbean355.zakbot

import net.dean.jraw.models.Comment
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update

private const val TelegramUsername = "ZakBagansBot"
private const val ChatId = "44692593"

private val bot = TelegramBot()

fun sendTelegramNotification(comment: Comment) {
    bot.execute(
        SendMessage(
            ChatId,
            """
                Comment by ${comment.author}:
                
                ${comment.body}
                
                https://www.reddit.com/r/GhostAdventures/comments/${comment.submissionFullName.substringAfter('_')}
            """.trimIndent()
        )
    )
}

private class TelegramBot : TelegramLongPollingBot() {
    override fun getBotToken(): String = System.getenv("TELEGRAM_TOKEN")
    override fun getBotUsername() = TelegramUsername
    override fun onUpdateReceived(update: Update?) = Unit
}