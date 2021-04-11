package com.github.mrbean355.zakbot

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update

private const val TelegramUsername = "ZakBagansBot"
private const val ChatId = "44692593"

interface TelegramNotifier {
    fun sendMessage(text: String)
}

@Component
@Profile("!dev")
class TelegramBot : TelegramLongPollingBot(), TelegramNotifier {

    override fun getBotToken(): String = System.getenv("TELEGRAM_TOKEN")

    override fun getBotUsername() = TelegramUsername

    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage()) {
            sendMessage("We want answers... answers...")
        }
    }

    override fun sendMessage(text: String) {
        execute(SendMessage(ChatId, text))
    }
}

@Component
@Profile("dev")
class StubTelegramNotifier : TelegramNotifier {

    override fun sendMessage(text: String) {
        println("[Telegram] $text")
    }
}