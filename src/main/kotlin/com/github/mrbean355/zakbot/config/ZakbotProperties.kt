package com.github.mrbean355.zakbot.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "zakbot")
data class ZakbotProperties(
    val replies: Replies = Replies(),
    val downvotes: Downvotes = Downvotes(),
    val moderation: Moderation = Moderation(),
) {
    data class Replies(
        val enabled: Boolean = true,
    )

    data class Downvotes(
        val enabled: Boolean = true,
        val deleteThreshold: Int = -1,
    )

    data class Moderation(
        val sensitivityThreshold: Float = 0.7f,
    )
}
