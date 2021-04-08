package com.github.mrbean355.zakbot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update

private const val TelegramUsername = "ZakBagansBot"
private const val ChatId = "44692593"

@Component
class TelegramBot : TelegramLongPollingBot() {
    override fun getBotToken(): String = System.getenv("TELEGRAM_TOKEN")
    override fun getBotUsername() = TelegramUsername
    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage()) {
            sendMessage("no u")
        }
    }
}

fun TelegramLongPollingBot.sendMessage(text: String) {
    execute(SendMessage(ChatId, text))
}