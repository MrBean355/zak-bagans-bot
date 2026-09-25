package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.TelegramNotifier
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

class ExceptionNotifierTest {
    @MockK
    private lateinit var telegramNotifier: TelegramNotifier

    private lateinit var exceptionNotifier: ExceptionNotifier

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        exceptionNotifier = ExceptionNotifier(telegramNotifier)
    }

    @Test
    fun testNotify_SendsFormattedMessageToTelegram() {
        val slot = slot<String>()
        every { telegramNotifier.sendMessage(capture(slot)) } returns Unit

        val exception = RuntimeException("Something exploded", IOException("Network failed"))
        exceptionNotifier.notify(exception, "Scheduled Task")

        verify(exactly = 1) { telegramNotifier.sendMessage(any()) }
        val message = slot.captured
        assertTrue(message.contains("*⚠️ Uncaught Exception* (Scheduled Task)"))
        assertTrue(message.contains("*IOException*: Network failed"))
        assertTrue(message.contains("```"))
    }

    @Test
    fun testNotify_WhenTelegramThrows_DoesNotRethrow() {
        every { telegramNotifier.sendMessage(any()) } throws RuntimeException("Telegram network error")

        // Should not throw
        exceptionNotifier.notify(RuntimeException("Root failure"), "Thread: main")
    }

    @Test
    fun testFormatMessage_IncludesSourceAndTruncatesStackTrace() {
        val exception = IllegalArgumentException("Bad input")
        val formatted = exceptionNotifier.formatMessage("HTTP Request", exception)

        assertTrue(formatted.contains("*⚠️ Uncaught Exception* (HTTP Request)"))
        assertTrue(formatted.contains("*IllegalArgumentException*: Bad input"))
    }
}
