package com.github.mrbean355.zakbot.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "telegram")
data class TelegramProperties(
    @field:NotBlank val token: String,
    @field:NotBlank val chatId: String,
)
