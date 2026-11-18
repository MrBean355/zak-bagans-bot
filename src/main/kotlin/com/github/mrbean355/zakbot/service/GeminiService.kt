package com.github.mrbean355.zakbot.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.mrbean355.zakbot.telegram.TelegramAction
import com.github.mrbean355.zakbot.telegram.TelegramNotifier
import com.github.mrbean355.zakbot.config.GeminiProperties
import com.github.mrbean355.zakbot.reddit.model.Comment
import com.github.mrbean355.zakbot.reddit.model.Submission
import com.github.mrbean355.zakbot.util.escapeTelegramMarkdown
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

    val isDryRun: Boolean
        get() = geminiProperties.dryRun

    fun evaluateSubmission(submission: Submission): ModerationResult {
        val input = buildString {
            append("Title: ").append(submission.title)
            if (!submission.selfText.isNullOrBlank()) {
                append("\n\nBody: ").append(submission.selfText)
            }
        }

        return generateStructuredOutput(
            systemInstruction = MODERATION_SYSTEM_INSTRUCTION,
            input = input,
            schema = MODERATION_RESPONSE_SCHEMA,
            responseType = ModerationResult::class.java,
            logDescription = "submission sensitivity (${submission.fullName})",
            telegramContextText = submission.title,
            telegramContextUrl = submission.url,
        ) ?: ModerationResult(sensitivity = 0f, reason = null)
    }

    fun evaluateComment(comment: Comment): ModerationResult {
        return generateStructuredOutput(
            systemInstruction = MODERATION_SYSTEM_INSTRUCTION,
            input = comment.body,
            schema = MODERATION_RESPONSE_SCHEMA,
            responseType = ModerationResult::class.java,
            logDescription = "comment sensitivity (${comment.fullName})",
            telegramContextText = comment.body,
            telegramContextUrl = comment.url,
        ) ?: ModerationResult(sensitivity = 0f, reason = null)
    }

    fun evaluateBotReply(comment: Comment): BotSentiment {
        return generateStructuredOutput(
            systemInstruction = BOT_REPLY_SYSTEM_INSTRUCTION,
            input = comment.body,
            schema = BOT_REPLY_RESPONSE_SCHEMA,
            responseType = SentimentResult::class.java,
            logDescription = "bot reply sentiment (${comment.fullName})",
            telegramContextText = comment.body,
            telegramContextUrl = comment.url,
        )?.sentiment ?: BotSentiment.NEUTRAL
    }

    private fun <T : Any> generateStructuredOutput(
        systemInstruction: String,
        input: String,
        schema: Map<String, Any>,
        responseType: Class<T>,
        logDescription: String,
        telegramContextText: String,
        telegramContextUrl: String,
    ): T? {
        if (!geminiProperties.enabled || geminiProperties.apiKey.isBlank()) {
            return null
        }

        val request = InteractionRequest(
            model = geminiProperties.model,
            systemInstruction = systemInstruction,
            input = input,
            store = false,
            responseFormat = ResponseFormat(
                type = "text",
                mimeType = "application/json",
                schema = schema,
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
                ?: return null

            jsonMapper.readValue(outputText, responseType)
        } catch (ex: Exception) {
            logger.warn("Failed to evaluate {}", logDescription, ex)
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
                    telegramContextText.escapeTelegramMarkdown(),
                    errorSummary,
                ),
                listOf(TelegramAction.OpenUrl(telegramContextUrl)),
            )
            null
        }
    }

    enum class BotSentiment {
        GOOD_BOT,
        BAD_BOT,
        NEUTRAL,
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ModerationResult(
        val sensitivity: Float,
        val reason: String?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SentimentResult(
        val sentiment: BotSentiment,
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
        private val MODERATION_SYSTEM_INSTRUCTION = """
            You are a content moderation assistant for a comedy Reddit bot on r/GhostAdventures that replies with funny Zak Bagans quotes.
            Rate the sensitivity (from 0.0 to 1.0) of a Reddit post or comment based on whether it discusses a genuinely serious, tragic, or sensitive real-life topic where a comedy quote reply would be inappropriate or disrespectful.
            Assign high sensitivity (0.7 to 1.0) ONLY for real-life serious topics such as:
            - Real-life death, obituary, mourning, or grief (e.g., a cast or crew member, fan, or family member passing away)
            - Severe real-life illness, injury, hospitalization, mental health crisis, self-harm, suicide, or abuse
            - Real-life violent crime, domestic violence, or tragedy involving real people
            Assign low sensitivity (0.0 to 0.2) for normal subreddit posts and comments, including:
            - Paranormal topics, ghosts, demons, possessions, hauntings, or historical deaths discussed as lore on the TV show Ghost Adventures
            - Memes, jokes, rants, episode discussions, or complaints about Zak Bagans or the crew
            If sensitivity is 0.5 or higher, provide a concise reason (under 100 characters). Otherwise set reason to null.
        """.trimIndent()

        private val MODERATION_RESPONSE_SCHEMA = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "sensitivity" to mapOf(
                    "type" to "number",
                    "minimum" to 0.0,
                    "maximum" to 1.0,
                    "description" to "Sensitivity score from 0.0 (normal/lighthearted content) to 1.0 (deeply serious, tragic, or sensitive real-life topic that a comedy quote bot should ignore).",
                ),
                "reason" to mapOf(
                    "type" to listOf("string", "null"),
                    "description" to "Short reason why the content is sensitive, or null if not sensitive.",
                ),
            ),
            "required" to listOf("sensitivity", "reason"),
        )

        private val BOT_REPLY_SYSTEM_INSTRUCTION = """
            You are a sentiment classifier for a comedy Reddit bot on r/GhostAdventures that replies with funny Zak Bagans quotes.
            Classify a user's comment replying directly to the bot into one of three categories:
            - GOOD_BOT: Clear praise, appreciation, or positive feedback directed at the bot (e.g., "good bot", "best bot ever", "I love this bot", "thank you zakbot").
            - BAD_BOT: Clear criticism, hostility, annoyance, or an opt-out request directed at the bot (e.g., "bad bot", "worst bot", "stupid bot", "shut up", "stop replying to me").
            - NEUTRAL: Anything else, including jokes playing along with the quote, general discussion about Zak Bagans or Ghost Adventures, questions, or ambiguous comments.
            Because BAD_BOT permanently opts the user out of future bot replies, only classify as BAD_BOT when the negative sentiment or opt-out intent toward the bot is clear.
        """.trimIndent()

        private val BOT_REPLY_RESPONSE_SCHEMA = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "sentiment" to mapOf(
                    "type" to "string",
                    "enum" to listOf("GOOD_BOT", "BAD_BOT", "NEUTRAL"),
                    "description" to "The sentiment classification of the user's reply toward the bot.",
                ),
            ),
            "required" to listOf("sentiment"),
        )
    }
}
