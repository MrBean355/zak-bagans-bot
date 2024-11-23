package com.github.mrbean355.zakbot.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MessagesKtTest {

    @Test
    fun testAsPlainText() {
        assertEquals("\\", "\\".asPlainText())
        assertEquals("*_*_*_", "\\*\\_\\*\\_\\*\\_".asPlainText())
        assertEquals("Some bold text.", "Some **bold** text.".asPlainText())
        assertEquals("Some italicised text.", "Some *italicised* text.".asPlainText())
        assertEquals("Invalid `markdown", "Invalid `markdown".asPlainText())
        assertEquals("test_username", "test\\_username".asPlainText())
        assertEquals("!command ignore_user victim", "!command ignore\\_user victim".asPlainText())
        assertEquals("Here are *multiple* _escapes_ in a comment.", "Here are \\*multiple\\* \\_escapes\\_ in a comment.".asPlainText())
    }

    @Test
    internal fun testCountOccurrences() {
        val input = "He found his art never progressed when he literally used his sweat and tears."

        assertEquals(1, input.countOccurrences("he"))
        assertEquals(2, input.countOccurrences("his"))
        assertEquals(1, input.countOccurrences("tears"))
    }
}