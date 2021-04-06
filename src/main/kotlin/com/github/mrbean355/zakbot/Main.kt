package com.github.mrbean355.zakbot

import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

fun main() {
    TelegramBotsApi(DefaultBotSession::class.java)
        .registerBot(TelegramBot)

    Thread.setDefaultUncaughtExceptionHandler { t, e ->
        runCatching {
            TelegramBot.sendMessage("Exception caught in ${t.name}:\n\n${e.stackTraceToString()}")
        }
    }

    ZakBot().start()
}