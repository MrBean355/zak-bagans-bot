package com.github.mrbean355.zakbot

import com.github.mrbean355.zakbot.util.getString
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import java.time.Duration
import java.time.Instant

private const val TelegramUsername = "ZakBagansBot"
private const val ChatId = "44692593"

interface TelegramNotifier {
    fun sendMessage(text: String)
}

@Component
@Profile("!dev")
class TelegramBot(
    private val applicationContext: ApplicationContext
) : TelegramLongPollingBot(), TelegramNotifier {

    override fun getBotToken(): String = System.getenv("TELEGRAM_TOKEN")

    override fun getBotUsername() = TelegramUsername

    override fun onUpdateReceived(update: Update) {
        if (update.message?.text == "!ping") {
            sendMessage(getString("telegram.bot_ping_response", AppVersion, getUptime()))
        }
    }

    override fun sendMessage(text: String) {
        execute(SendMessage(ChatId, text))
    }

    @OptIn(ExperimentalStdlibApi::class)
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

@Component
@Profile("dev")
class StubTelegramNotifier : TelegramNotifier {
    private val logger = LoggerFactory.getLogger(StubTelegramNotifier::class.java)

    override fun sendMessage(text: String) {
        logger.info("[Telegram] $text")
    }
}