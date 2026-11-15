package com.github.mrbean355.zakbot.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class GeminiConfigTest {
    private val config = GeminiConfig()

    @Test
    fun testGeminiRestClient_ConfiguresBaseUrlAndApiKeyHeader() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val properties = GeminiProperties(apiKey = "test-gemini-key", model = "gemini-3.5-flash-lite")
        val client = config.geminiRestClient(builder, properties)

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1/interactions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("x-goog-api-key", "test-gemini-key"))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))

        val response = client.post().uri("/v1/interactions").retrieve().body(String::class.java)

        assertEquals("ok", response)
        server.verify()
    }
}
