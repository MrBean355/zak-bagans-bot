package com.github.mrbean355.zakbot.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.context.support.ResourceBundleMessageSource

internal class StringsKtTest {
    private val messageSource = ResourceBundleMessageSource().apply {
        setBasename("strings")
        setDefaultEncoding("UTF-8")
        setUseCodeAsDefaultMessage(true)
        setAlwaysUseMessageFormat(true)
    }

    @Test
    internal fun testGetString_WithoutFormatArgs() {
        assertEquals(
            "I'm sorry you feel that way. I won't reply to your posts or comments anymore.",
            messageSource.getString("reddit.new_user_ignored"),
        )
    }

    @Test
    internal fun testGetString_WithFormatArgs() {
        assertEquals("5 days", messageSource.getString("telegram.bot_uptime_days", 5))
    }

    @Test
    internal fun testGetString_KeyNotFound_ReturnsKey() {
        assertEquals("nonexistent.key.name", messageSource.getString("nonexistent.key.name"))
    }
}
