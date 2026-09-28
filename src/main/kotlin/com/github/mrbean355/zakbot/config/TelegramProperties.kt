package com.github.mrbean355.zakbot.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "telegram")
data class TelegramProperties(
    val token: String = "",
    val chatId: String = "44692593",
)
