package com.github.mrbean355.zakbot.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.mrbean355.zakbot.TelegramNotifier
import com.github.mrbean355.zakbot.config.GeminiProperties
import com.github.mrbean355.zakbot.reddit.model.Submission
import com.github.mrbean355.zakbot.util.getString
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper

@Service
class GeminiService(
    @Qualifier("geminiRestClient") private val client: RestClient,
    private val geminiProperties: GeminiProperties,
    private val telegramNotifier: TelegramNotifier,
) {
    private val logger = LoggerFactory.getLogger(GeminiService::class.java)
    private val jsonMapper = JsonMapper.builder().build()

    fun evaluateSubmission(submission: Submission): ModerationResult {
        if (!geminiProperties.enabled || geminiProperties.apiKey.isBlank()) {
            return ModerationResult(sensitivity = 0f, reason = null)
        }

        val input = buildString {
            append("Title: ").append(submission.title)
            if (!submission.selfText.isNullOrBlank()) {
                append("\n\nBody: ").append(submission.selfText)
            }
        }

        val request = InteractionRequest(
            model = geminiProperties.model,
            systemInstruction = SYSTEM_INSTRUCTION,
            input = input,
            store = false,
            responseFormat = ResponseFormat(
                type = "text",
                mimeType = "application/json",
                schema = RESPONSE_SCHEMA,
            ),
        )

        return try {
            val response = client.post()
                .uri("/v1/interactions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(InteractionResponse::class.java)

            val outputText = response?.steps
                ?.lastOrNull { it.type == "model_output" }
                ?.content
                ?.lastOrNull { it.type == "text" }
                ?.text
                ?: return ModerationResult(sensitivity = 0f, reason = null)

            jsonMapper.readValue(outputText, ModerationResult::class.java)
        } catch (ex: Exception) {
            logger.warn("Failed to evaluate submission sensitivity: {}", submission.fullName, ex)
            val errorDetail = ex.message?.lineSequence()?.firstOrNull()?.take(150)?.replace("`", "'")
            val errorSummary = buildString {
                append(ex::class.simpleName ?: "Exception")
                if (!errorDetail.isNullOrBlank()) {
                    append(": ").append(errorDetail)
                }
            }
            telegramNotifier.sendMessage(
                getString(
                    "telegram.gemini_evaluation_failed",
                    submission.title,
                    errorSummary,
                    submission.url,
                )
            )
            ModerationResult(sensitivity = 0f, reason = null)
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ModerationResult(
        val sensitivity: Float,
        val reason: String?,
    )

    data class InteractionRequest(
        val model: String,
        @JsonProperty("system_instruction") val systemInstruction: String,
        val input: String,
        val store: Boolean,
        @JsonProperty("response_format") val responseFormat: ResponseFormat,
    )

    data class ResponseFormat(
        val type: String,
        @JsonProperty("mime_type") val mimeType: String,
        val schema: Map<String, Any>,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class InteractionResponse(
        val steps: List<InteractionStep>?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class InteractionStep(
        val type: String?,
        val content: List<InteractionContent>?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class InteractionContent(
        val type: String?,
        val text: String?,
    )

    private companion object {
        private val SYSTEM_INSTRUCTION = """
            You are a content moderation assistant for a comedy Reddit bot on r/GhostAdventures that replies with funny Zak Bagans quotes.
            Rate the sensitivity (from 0.0 to 1.0) of a Reddit submission based on whether it discusses a genuinely serious, tragic, or sensitive real-life topic where a comedy quote reply would be inappropriate or disrespectful.
            Assign high sensitivity (0.7 to 1.0) ONLY for real-life serious topics such as:
            - Real-life death, obituary, mourning, or grief (e.g., a cast or crew member, fan, or family member passing away)
            - Severe real-life illness, injury, hospitalization, mental health crisis, self-harm, suicide, or abuse
            - Real-life violent crime, domestic violence, or tragedy involving real people
            Assign low sensitivity (0.0 to 0.2) for normal subreddit posts, including:
            - Paranormal topics, ghosts, demons, possessions, hauntings, or historical deaths discussed as lore on the TV show Ghost Adventures
            - Memes, jokes, rants, episode discussions, or complaints about Zak Bagans or the crew
            If sensitivity is 0.5 or higher, provide a concise reason (under 100 characters). Otherwise set reason to null.
        """.trimIndent()

        private val RESPONSE_SCHEMA = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "sensitivity" to mapOf(
                    "type" to "number",
                    "minimum" to 0.0,
                    "maximum" to 1.0,
                    "description" to "Sensitivity score from 0.0 (normal/lighthearted post) to 1.0 (deeply serious, tragic, or sensitive real-life topic that a comedy quote bot should ignore).",
                ),
                "reason" to mapOf(
                    "type" to listOf("string", "null"),
                    "description" to "Short reason why the post is sensitive, or null if not sensitive.",
                ),
            ),
            "required" to listOf("sensitivity", "reason"),
        )
    }
}
