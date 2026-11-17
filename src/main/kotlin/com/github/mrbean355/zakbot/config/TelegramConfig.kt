package com.github.mrbean355.zakbot.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.generics.TelegramClient

@Configuration
class TelegramConfig {

    @Bean
    fun telegramClient(properties: TelegramProperties): TelegramClient {
        return OkHttpTelegramClient(properties.token)
    }
}
