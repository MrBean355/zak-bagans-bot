package com.github.mrbean355.zakbot.config

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NegativeOrZero
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "zakbot")
data class ZakbotProperties(
    @field:Valid val replies: Replies = Replies(),
    @field:Valid val downvotes: Downvotes = Downvotes(),
    @field:Valid val moderation: Moderation = Moderation(),
) {
    data class Replies(
        val enabled: Boolean = true,
    )

    data class Downvotes(
        val enabled: Boolean = true,
        @field:NegativeOrZero val deleteThreshold: Int = -3,
    )

    data class Moderation(
        @field:DecimalMin("0.0")
        @field:DecimalMax("1.0")
        val sensitivityThreshold: Float = 0.7f,
    )
}
