package com.github.mrbean355.zakbot.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class GeminiConfig {

    @Bean
    fun geminiRestClient(
        builder: RestClient.Builder,
        geminiProperties: GeminiProperties,
    ): RestClient {
        return builder.clone()
            .baseUrl("https://generativelanguage.googleapis.com")
            .defaultHeader("x-goog-api-key", geminiProperties.apiKey)
            .build()
    }
}
