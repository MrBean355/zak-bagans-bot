package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.TelegramNotifier
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ExceptionNotifier(
    private val telegramNotifier: TelegramNotifier
) {
    private val logger = LoggerFactory.getLogger(ExceptionNotifier::class.java)

    fun notify(throwable: Throwable, source: String) {
        logger.error("Uncaught exception in $source", throwable)
        try {
            telegramNotifier.sendMessage(formatMessage(source, throwable))
        } catch (t: Throwable) {
            logger.error("Failed to send Telegram notification for exception", t)
        }
    }

    internal fun formatMessage(source: String, throwable: Throwable): String {
        val rootCause = generateSequence(throwable) { it.cause }.last()
        val exceptionName = rootCause::class.simpleName ?: "Exception"
        val message = rootCause.message?.take(200) ?: "No message"
        val stackTrace = throwable.stackTraceToString()
            .lines()
            .take(8)
            .joinToString("\n")
            .take(800)

        return buildString {
            append("*⚠️ Uncaught Exception* ($source)\n\n")
            append("*$exceptionName*: $message\n\n")
            append("```\n")
            append(stackTrace)
            append("\n```")
        }
    }
}
