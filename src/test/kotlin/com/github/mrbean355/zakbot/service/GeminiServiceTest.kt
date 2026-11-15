package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.TelegramNotifier
import com.github.mrbean355.zakbot.config.GeminiProperties
import com.github.mrbean355.zakbot.reddit.model.Submission
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.Instant

class GeminiServiceTest {
    private lateinit var server: MockRestServiceServer
    private lateinit var client: RestClient

    @MockK
    private lateinit var telegramNotifier: TelegramNotifier

    private lateinit var service: GeminiService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        justRun { telegramNotifier.sendMessage(any()) }

        val builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com")
        server = MockRestServiceServer.bindTo(builder).bufferContent().build()
        client = builder.build()

        service = GeminiService(
            client = client,
            geminiProperties = GeminiProperties(apiKey = "test-api-key", model = "gemini-3.5-flash-lite"),
            telegramNotifier = telegramNotifier,
        )
    }

    @Test
    fun testEvaluateSubmission_WhenApiKeyBlank_ReturnsZeroSensitivityWithoutCallingApi() {
        val unconfiguredService = GeminiService(
            client = client,
            geminiProperties = GeminiProperties(apiKey = ""),
            telegramNotifier = telegramNotifier,
        )
        val submission = createSubmission(title = "RIP to a great investigator")

        val result = unconfiguredService.evaluateSubmission(submission)

        assertEquals(0f, result.sensitivity)
        assertNull(result.reason)
        verify(exactly = 0) { telegramNotifier.sendMessage(any()) }
        server.verify()
    }

    @Test
    fun testEvaluateSubmission_WhenSensitiveSubmission_ReturnsSensitivityAndReason() {
        val responseJson = """
            {
              "id": "int_123",
              "steps": [
                {
                  "type": "thought",
                  "signature": "sig_1"
                },
                {
                  "type": "model_output",
                  "content": [
                    {
                      "type": "text",
                      "text": "{\"sensitivity\":0.92,\"reason\":\"Cast member passing away\"}"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1/interactions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.model").value("gemini-3.5-flash-lite"))
            .andExpect(jsonPath("$.store").value(false))
            .andExpect(jsonPath("$.input").value("Title: Sad news today\n\nBody: Rest in peace"))
            .andExpect(jsonPath("$.response_format.type").value("text"))
            .andExpect(jsonPath("$.response_format.mime_type").value("application/json"))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON))

        val submission = createSubmission(title = "Sad news today", selfText = "Rest in peace")
        val result = service.evaluateSubmission(submission)

        assertEquals(0.92f, result.sensitivity)
        assertEquals("Cast member passing away", result.reason)
        verify(exactly = 0) { telegramNotifier.sendMessage(any()) }
        server.verify()
    }

    @Test
    fun testEvaluateSubmission_WhenNormalSubmissionWithoutSelfText_ReturnsLowSensitivity() {
        val responseJson = """
            {
              "id": "int_456",
              "steps": [
                {
                  "type": "model_output",
                  "content": [
                    {
                      "type": "text",
                      "text": "{\"sensitivity\":0.05,\"reason\":null}"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1/interactions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.input").value("Title: Did you see Zak's reaction to Zozo?"))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON))

        val submission = createSubmission(title = "Did you see Zak's reaction to Zozo?", selfText = null)
        val result = service.evaluateSubmission(submission)

        assertEquals(0.05f, result.sensitivity)
        assertNull(result.reason)
        verify(exactly = 0) { telegramNotifier.sendMessage(any()) }
        server.verify()
    }

    @Test
    fun testEvaluateSubmission_WhenStepsEmpty_ReturnsZeroSensitivity() {
        val responseJson = """{ "id": "int_789", "steps": [] }"""

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1/interactions"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON))

        val submission = createSubmission(title = "Normal post", selfText = "   ")
        val result = service.evaluateSubmission(submission)

        assertEquals(0f, result.sensitivity)
        assertNull(result.reason)
        verify(exactly = 0) { telegramNotifier.sendMessage(any()) }
        server.verify()
    }

    @Test
    fun testEvaluateSubmission_WhenApiFails_SendsTelegramNotificationAndReturnsZeroSensitivity() {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1/interactions"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS))

        val submission = createSubmission(title = "Post title")
        val result = service.evaluateSubmission(submission)

        assertEquals(0f, result.sensitivity)
        assertNull(result.reason)
        verify(exactly = 1) {
            telegramNotifier.sendMessage(
                match {
                    it.contains("Gemini Evaluation Failed") &&
                        it.contains("Post title") &&
                        it.contains("429") &&
                        it.contains(submission.url)
                }
            )
        }
        server.verify()
    }

    @Test
    fun testEvaluateSubmission_WhenModelOutputIsInvalidJson_SendsTelegramNotificationAndReturnsZeroSensitivity() {
        val responseJson = """
            {
              "id": "int_bad",
              "steps": [
                {
                  "type": "model_output",
                  "content": [
                    {
                      "type": "text",
                      "text": "not-valid-json"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1/interactions"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON))

        val submission = createSubmission(title = "Post title")
        val result = service.evaluateSubmission(submission)

        assertEquals(0f, result.sensitivity)
        assertNull(result.reason)
        verify(exactly = 1) {
            telegramNotifier.sendMessage(
                match {
                    it.contains("Gemini Evaluation Failed") &&
                        it.contains("Post title") &&
                        it.contains(submission.url)
                }
            )
        }
        server.verify()
    }

    private fun createSubmission(
        id: String = "sub1",
        author: String = "user1",
        title: String = "Title",
        selfText: String? = null,
    ) = Submission(
        id = id,
        fullName = "t3_$id",
        author = author,
        created = Instant.now(),
        url = "https://reddit.com/r/GhostAdventures/comments/$id",
        title = title,
        selfText = selfText,
    )
}
