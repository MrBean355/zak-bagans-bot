package com.github.mrbean355.zakbot.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "reddit")
data class RedditProperties(
    @field:NotBlank val accountPassword: String,
    @field:NotBlank val clientSecret: String,
)
