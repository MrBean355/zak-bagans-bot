package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.util.readResourceFileLines
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

internal class UnderstandPhraseTest {

    @Test
    internal fun testPriority() {
        assertEquals(1, UnderstandPhrase().priority)
    }

    @Test
    internal fun testResponses() {
        val responses = mockk<List<String>>()
        mockkStatic(::readResourceFileLines)
        every { readResourceFileLines(any()) } returns responses

        val actual = UnderstandPhrase().responses

        assertSame(responses, actual)
        verify {
            readResourceFileLines("phrases/understand.txt")
        }
    }

    @Test
    internal fun testGetReplyChance_DoesNotContainUnderstand_ReturnsZeroPercent() {
        val actual = UnderstandPhrase().getReplyChance("hello world!")

        assertEquals(0f, actual)
    }

    @Test
    internal fun testGetReplyChance_ContainsUnderstandOnce_ReturnsFiftyPercent() {
        val actual = UnderstandPhrase().getReplyChance("we will never understand!")

        assertEquals(0.5f, actual)
    }

    @Test
    internal fun testGetReplyChance_ContainsUnderstandMultipleTimes_ReturnsZeroPercent() {
        val actual = UnderstandPhrase().getReplyChance("we will never understand... understand!")

        assertEquals(0f, actual)
    }
}