package com.github.mrbean355.zakbot.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class StringsKtTest {

    @Test
    internal fun testGetString_WithoutFormatArgs() {
        assertEquals("Hello world!", getString("test.string.first"))
    }

    @Test
    internal fun testGetString_WithFormatArgs() {
        assertEquals("Hello Zak!", getString("test.string.second", "Zak"))
    }
}