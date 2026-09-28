package com.github.mrbean355.zakbot.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "zakbot")
data class ZakbotProperties(
    val replies: Replies = Replies(),
) {
    data class Replies(
        val enabled: Boolean = true,
    )
}
