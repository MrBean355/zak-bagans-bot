package com.github.mrbean355.zakbot

import com.github.mrbean355.zakbot.util.getString
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.generics.TelegramClient
import java.time.Duration
import java.time.Instant

private const val ChatId = "44692593"

interface TelegramNotifier {
    fun sendMessage(text: String)
}

@Component
@Profile("!dev")
class TelegramBot(
    private val applicationContext: ApplicationContext,
    @Value($$"${TELEGRAM_TOKEN}") private val botToken: String,
) : SpringLongPollingBot, LongPollingUpdateConsumer, TelegramNotifier {

    private val telegramClient: TelegramClient = OkHttpTelegramClient(botToken)

    override fun getBotToken(): String = botToken

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer = this

    override fun consume(updates: List<Update>) {
        for (update in updates) {
            if (update.message?.text == "/ping") {
                sendMessage(getString("telegram.bot_ping_response", AppVersion, getUptime()))
            }
        }
    }

    override fun sendMessage(text: String) {
        telegramClient.execute(
            SendMessage.builder()
                .chatId(ChatId)
                .text(text)
                .parseMode(ParseMode.MARKDOWN)
                .linkPreviewOptions(LinkPreviewOptions.builder().isDisabled(true).build())
                .build()
        )
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

@Component
@Profile("dev")
class StubTelegramNotifier : TelegramNotifier {
    private val logger = LoggerFactory.getLogger(StubTelegramNotifier::class.java)

    override fun sendMessage(text: String) {
        logger.info("[Telegram] $text")
    }
}