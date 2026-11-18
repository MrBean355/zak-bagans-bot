package com.github.mrbean355.zakbot.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "admin")
data class AdminProperties(
    @field:NotBlank val username: String = "admin",
    val password: String = "",
)
