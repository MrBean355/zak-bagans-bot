package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.util.readResourceFileLines
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

internal class FeelingPhraseTest {

    @Test
    internal fun testPriority() {
        assertEquals(8, FeelingPhrase().priority)
    }

    @Test
    internal fun testResponses() {
        val responses = mockk<List<String>>()
        mockkStatic(::readResourceFileLines)
        every { readResourceFileLines(any()) } returns responses

        val actual = FeelingPhrase().responses

        assertSame(responses, actual)
        verify {
            readResourceFileLines("phrases/feeling.txt")
        }
    }

    @Test
    internal fun testGetReplyChance_DoesNotContainPhrase_ReturnsZeroPercent() {
        val actual = FeelingPhrase().getReplyChance("hello world!")

        assertEquals(0f, actual)
    }

    @Test
    internal fun testGetReplyChance_ContainsFirstPhrase_ReturnsFiftyPercent() {
        val actual = FeelingPhrase().getReplyChance("i feel filled with rage")

        assertEquals(0.5f, actual)
    }

    @Test
    internal fun testGetReplyChance_ContainsSecondPhrase_ReturnsFiftyPercent() {
        val actual = FeelingPhrase().getReplyChance("i'm feeling overwhelmed with sadness")

        assertEquals(0.5f, actual)
    }
}