package com.github.mrbean355.zakbot.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "admin")
data class AdminProperties(
    val username: String = "admin",
    val password: String = "",
)
