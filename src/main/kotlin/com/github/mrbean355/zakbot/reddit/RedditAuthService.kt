package com.github.mrbean355.zakbot.reddit

import com.github.mrbean355.zakbot.BotClientId
import com.github.mrbean355.zakbot.BotUsername
import com.github.mrbean355.zakbot.reddit.dto.RedditTokenResponse
import com.github.mrbean355.zakbot.util.SystemClock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

@Service
class RedditAuthService(
    @Value($$"${BOT_ACCOUNT_PASSWORD:}") private val botAccountPassword: String,
    @Value($$"${BOT_CLIENT_SECRET:}") private val botClientSecret: String,
    private val systemClock: SystemClock,
    @Qualifier("redditAuthRestClient") private val authClient: RestClient,
) {
    private val logger = LoggerFactory.getLogger(RedditAuthService::class.java)

    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var expiresAtMillis: Long = 0L

    @Synchronized
    fun getAccessToken(): String {
        val now = systemClock.currentTimeMillis
        val token = cachedToken
        if (token != null && now < expiresAtMillis - REFRESH_BUFFER_MILLIS) {
            return token
        }

        logger.info("Requesting new Reddit OAuth access token...")
        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "password")
            add("username", BotUsername)
            add("password", botAccountPassword)
        }

        val response = authClient.post()
            .uri("/api/v1/access_token")
            .headers { it.setBasicAuth(BotClientId, botClientSecret) }
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .body(RedditTokenResponse::class.java)
            ?: error("Empty response received from Reddit OAuth endpoint")

        cachedToken = response.accessToken
        expiresAtMillis = now + (response.expiresIn * 1000)
        logger.info("Successfully acquired Reddit access token (expires in {}s)", response.expiresIn)
        return response.accessToken
    }

    @Synchronized
    fun invalidateToken() {
        logger.info("Invalidating cached Reddit access token")
        cachedToken = null
        expiresAtMillis = 0L
    }

    private companion object {
        private const val REFRESH_BUFFER_MILLIS = 60_000L
    }
}
