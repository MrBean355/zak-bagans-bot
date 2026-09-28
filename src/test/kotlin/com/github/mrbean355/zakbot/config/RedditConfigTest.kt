package com.github.mrbean355.zakbot.config

import com.github.mrbean355.zakbot.AuthorUsername
import com.github.mrbean355.zakbot.BotUsername
import com.github.mrbean355.zakbot.reddit.RedditAuthService
import com.github.mrbean355.zakbot.reddit.RedditLoggingInterceptor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.info.BuildProperties
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.util.Properties
import java.util.function.Consumer

class RedditConfigTest {
    private val buildProperties = BuildProperties(Properties().apply {
        setProperty("version", "3.0.0")
    })
    private val config = RedditConfig(buildProperties)
    private val environment: Environment = mockk()

    @BeforeEach
    fun setUp() {
        every { environment.acceptsProfiles(Profiles.of("dev")) } returns false
    }

    @Test
    fun testClientHttpRequestFactory_WhenDevProfileActive_ReturnsBufferingFactory() {
        every { environment.acceptsProfiles(Profiles.of("dev")) } returns true

        val factory = config.clientHttpRequestFactory(environment)

        assertTrue(factory is BufferingClientHttpRequestFactory)
    }

    @Test
    fun testClientHttpRequestFactory_WhenDevProfileNotActive_ReturnsJdkFactory() {
        every { environment.acceptsProfiles(Profiles.of("dev")) } returns false

        val factory = config.clientHttpRequestFactory(environment)

        assertTrue(factory is JdkClientHttpRequestFactory)
    }

    @Test
    fun testRestClientBuilder_WhenLoggingInterceptorAvailable_AddsInterceptor() {
        val factory: ClientHttpRequestFactory = mockk()
        val loggingInterceptor = RedditLoggingInterceptor()
        val provider = mockk<ObjectProvider<RedditLoggingInterceptor>>()
        every { provider.ifAvailable(any()) } answers {
            firstArg<Consumer<RedditLoggingInterceptor>>().accept(loggingInterceptor)
        }

        val builder = config.restClientBuilder(factory, provider)
        val interceptors = ReflectionTestUtils.getField(builder, "interceptors") as? List<*>

        assertTrue(interceptors?.any { it is RedditLoggingInterceptor } == true)
    }

    @Test
    fun testRestClientBuilder_WhenLoggingInterceptorNotAvailable_DoesNotAddInterceptor() {
        val factory: ClientHttpRequestFactory = mockk()
        val provider = mockk<ObjectProvider<RedditLoggingInterceptor>>()
        every { provider.ifAvailable(any()) } answers {}

        val builder = config.restClientBuilder(factory, provider)
        val interceptors = ReflectionTestUtils.getField(builder, "interceptors") as? List<*>

        assertFalse(interceptors?.any { it is RedditLoggingInterceptor } == true)
    }

    @Test
    fun testGetUserAgentHeader_WhenDevProfileActive_AppendsDevSuffix() {
        every { environment.acceptsProfiles(Profiles.of("dev")) } returns true

        val header = config.getUserAgentHeader(environment)

        assertEquals("bot:$BotUsername:3.0.0-dev (by /u/$AuthorUsername)", header)
    }

    @Test
    fun testGetUserAgentHeader_WhenDevProfileNotActive_ReturnsStandardHeader() {
        every { environment.acceptsProfiles(Profiles.of("dev")) } returns false

        val header = config.getUserAgentHeader(environment)

        assertEquals("bot:$BotUsername:3.0.0 (by /u/$AuthorUsername)", header)
    }

    @Test
    fun testRedditAuthRestClient_ConfiguresBaseUrlAndUserAgent() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val authClient = config.redditAuthRestClient(builder, environment)

        server.expect(requestTo("https://www.reddit.com/test"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("User-Agent", "bot:$BotUsername:3.0.0 (by /u/$AuthorUsername)"))
            .andRespond(withSuccess("auth-ok", MediaType.TEXT_PLAIN))

        val response = authClient.get().uri("/test").retrieve().body(String::class.java)

        assertEquals("auth-ok", response)
        server.verify()
    }

    @Test
    fun testRedditRestClient_WhenAuthorized_SetsBearerAuthAndDoesNotRetry() {
        val authService: RedditAuthService = mockk(relaxUnitFun = true)
        every { authService.getAccessToken() } returns "valid-token"

        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = config.redditRestClient(authService, builder, environment)

        server.expect(requestTo("https://oauth.reddit.com/test"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer valid-token"))
            .andRespond(withSuccess("success", MediaType.TEXT_PLAIN))

        val response = client.get().uri("/test").retrieve().body(String::class.java)

        assertEquals("success", response)
        verify(exactly = 0) { authService.invalidateToken() }
        server.verify()
    }

    @Test
    fun testRedditRestClient_WhenUnauthorized_InvalidatesTokenAndRetries() {
        val authService: RedditAuthService = mockk(relaxUnitFun = true)
        every { authService.getAccessToken() } returnsMany listOf("expired-token", "fresh-token")

        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).bufferContent().build()
        val client = config.redditRestClient(authService, builder, environment)

        server.expect(requestTo("https://oauth.reddit.com/test"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer expired-token"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED))

        server.expect(requestTo("https://oauth.reddit.com/test"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer fresh-token"))
            .andRespond(withSuccess("recovered", MediaType.TEXT_PLAIN))

        val response = client.get().uri("/test").retrieve().body(String::class.java)

        assertEquals("recovered", response)
        verify(exactly = 1) { authService.invalidateToken() }
        server.verify()
    }
}

