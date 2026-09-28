package com.github.mrbean355.zakbot.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "reddit")
data class RedditProperties(
    val accountPassword: String = "",
    val clientSecret: String = "",
)
