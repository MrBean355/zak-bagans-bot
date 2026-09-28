package com.github.mrbean355.zakbot.reddit

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

@Component
@Profile("dev")
class RedditLoggingInterceptor : ClientHttpRequestInterceptor {
    private val logger = LoggerFactory.getLogger(RedditLoggingInterceptor::class.java)

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val start = System.currentTimeMillis()
        logger.info("[HTTP] --> {} {}", request.method, request.uri)

        if (body.isNotEmpty()) {
            val bodyString = String(body, StandardCharsets.UTF_8)
            val sanitized = sanitize(bodyString)
            if (logger.isDebugEnabled) {
                logger.debug("[HTTP] --> Body: {}", sanitized)
            } else {
                logger.info("[HTTP] --> Body: {}", sanitized)
            }
        }

        val response = execution.execute(request, body)
        val duration = System.currentTimeMillis() - start

        val responseBody = response.body.bufferedReader().readText()
        if (response.statusCode.isError) {
            logger.warn(
                "[HTTP] <-- {} {} ({}ms): {}",
                response.statusCode.value(),
                response.statusText,
                duration,
                responseBody
            )
        } else {
            logger.info("[HTTP] <-- {} {} ({}ms)", response.statusCode.value(), response.statusText, duration)
            if (responseBody.isNotBlank()) {
                if (logger.isDebugEnabled) {
                    logger.debug("[HTTP] <-- Body: {}", responseBody)
                } else {
                    val preview = if (responseBody.length > 300) {
                        "${responseBody.take(300)}... [truncated ${responseBody.length} chars]"
                    } else {
                        responseBody
                    }
                    logger.info("[HTTP] <-- Body: {}", preview)
                }
            }
        }

        return response
    }

    private fun sanitize(content: String): String {
        return content
            .replace(FORM_PASSWORD_REGEX, "password=***")
            .replace(JSON_PASSWORD_REGEX, "\"password\":\"***\"")
    }

    private companion object {
        private val FORM_PASSWORD_REGEX = Regex("password=[^&]+", RegexOption.IGNORE_CASE)
        private val JSON_PASSWORD_REGEX = Regex("\"password\"\\s*:\\s*\"[^\"]+\"", RegexOption.IGNORE_CASE)
    }
}
