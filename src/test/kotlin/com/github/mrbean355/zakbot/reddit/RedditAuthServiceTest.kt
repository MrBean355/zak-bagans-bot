package com.github.mrbean355.zakbot.reddit

import com.github.mrbean355.zakbot.util.SystemClock
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class RedditAuthServiceTest {
    private lateinit var server: MockRestServiceServer
    private lateinit var systemClock: SystemClock
    private lateinit var authService: RedditAuthService

    @BeforeEach
    fun setUp() {
        systemClock = mockk()
        val builder = RestClient.builder().baseUrl("https://www.reddit.com")
        server = MockRestServiceServer.bindTo(builder).build()

        authService = RedditAuthService(
            botAccountPassword = "test-password",
            botClientSecret = "test-secret",
            systemClock = systemClock,
            authClient = builder.build(),
        )
    }

    @Test
    fun testGetAccessToken_WhenNoCachedToken_RequestsNewToken() {
        every { systemClock.currentTimeMillis } returns 1_000_000L

        val json = """
            {
              "access_token": "token-xyz-123",
              "token_type": "bearer",
              "expires_in": 3600,
              "scope": "*"
            }
        """.trimIndent()

        server.expect(requestTo("https://www.reddit.com/api/v1/access_token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", org.hamcrest.Matchers.startsWith("Basic ")))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().formDataContains(mapOf(
                "grant_type" to "password",
                "username" to "ZakBagansBot",
                "password" to "test-password"
            )))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON))

        val token = authService.getAccessToken()

        assertEquals("token-xyz-123", token)
        server.verify()
    }

    @Test
    fun testGetAccessToken_WhenTokenValidInCache_ReturnsCachedWithoutNetworkCall() {
        every { systemClock.currentTimeMillis } returns 1_000_000L

        val json = """
            {
              "access_token": "cached-token",
              "token_type": "bearer",
              "expires_in": 3600,
              "scope": "*"
            }
        """.trimIndent()

        server.expect(requestTo("https://www.reddit.com/api/v1/access_token"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON))

        val firstToken = authService.getAccessToken()
        assertEquals("cached-token", firstToken)

        // 10 minutes later (still well within 3600s - 60s buffer)
        every { systemClock.currentTimeMillis } returns 1_600_000L
        val secondToken = authService.getAccessToken()
        assertEquals("cached-token", secondToken)

        server.verify()
    }

    @Test
    fun testInvalidateToken_ForcesNewTokenFetch() {
        every { systemClock.currentTimeMillis } returns 1_000_000L

        val firstJson = """{"access_token": "token-1", "token_type": "bearer", "expires_in": 3600, "scope": "*"}"""
        val secondJson = """{"access_token": "token-2", "token_type": "bearer", "expires_in": 3600, "scope": "*"}"""

        server.expect(requestTo("https://www.reddit.com/api/v1/access_token"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(firstJson, MediaType.APPLICATION_JSON))

        server.expect(requestTo("https://www.reddit.com/api/v1/access_token"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(secondJson, MediaType.APPLICATION_JSON))

        assertEquals("token-1", authService.getAccessToken())

        authService.invalidateToken()

        assertEquals("token-2", authService.getAccessToken())

        server.verify()
    }
}
