package com.github.mrbean355.zakbot.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "gemini")
data class GeminiProperties(
    val apiKey: String = "",
    @field:NotBlank val model: String = "gemini-3.5-flash-lite",
    val enabled: Boolean = true,
    val dryRun: Boolean = true,
)
