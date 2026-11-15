package com.github.mrbean355.zakbot.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "gemini")
data class GeminiProperties(
    val apiKey: String = "",
    val model: String = "gemini-3.5-flash-lite",
    val enabled: Boolean = true,
    val dryRun: Boolean = true,
)
