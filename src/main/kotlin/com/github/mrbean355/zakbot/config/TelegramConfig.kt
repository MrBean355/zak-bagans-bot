package com.github.mrbean355.zakbot.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.generics.TelegramClient

@Configuration
@Profile("!dev")
class TelegramConfig {

    @Bean
    fun telegramClient(properties: TelegramProperties): TelegramClient {
        return OkHttpTelegramClient(properties.token)
    }
}
