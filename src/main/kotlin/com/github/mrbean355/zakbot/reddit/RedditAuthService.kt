package com.github.mrbean355.zakbot.reddit

import com.github.mrbean355.zakbot.BotClientId
import com.github.mrbean355.zakbot.BotUsername
import com.github.mrbean355.zakbot.config.RedditProperties
import com.github.mrbean355.zakbot.reddit.dto.RedditTokenResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import java.time.Clock

@Service
class RedditAuthService(
    private val redditProperties: RedditProperties,
    private val clock: Clock,
    @Qualifier("redditAuthRestClient") private val authClient: RestClient,
) {
    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var expiresAtMillis: Long = 0L

    @Synchronized
    fun getAccessToken(): String {
        val now = clock.millis()
        val token = cachedToken
        if (token != null && now < expiresAtMillis - REFRESH_BUFFER_MILLIS) {
            return token
        }

        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "password")
            add("username", BotUsername)
            add("password", redditProperties.accountPassword)
        }

        val response = authClient.post()
            .uri("/api/v1/access_token")
            .headers { it.setBasicAuth(BotClientId, redditProperties.clientSecret) }
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .body(RedditTokenResponse::class.java)
            ?: error("Empty response received from Reddit OAuth endpoint")

        cachedToken = response.accessToken
        expiresAtMillis = now + (response.expiresIn * 1000)
        return response.accessToken
    }

    @Synchronized
    fun invalidateToken() {
        cachedToken = null
        expiresAtMillis = 0L
    }

    private companion object {
        private const val REFRESH_BUFFER_MILLIS = 60_000L
    }
}
