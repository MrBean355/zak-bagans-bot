package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.util.readResourceFileLines
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

internal class GenericPhraseTest {

    @Test
    internal fun testPriority() {
        assertEquals(0, GenericPhrase().priority)
    }

    @Test
    internal fun testResponses() {
        val responses = mockk<List<String>>()
        mockkStatic(::readResourceFileLines)
        every { readResourceFileLines(any()) } returns responses

        val actual = GenericPhrase().responses

        assertSame(responses, actual)
        verify {
            readResourceFileLines("phrases/generic.txt")
        }
    }

    @Test
    internal fun testGetReplyChance_ReturnsOneHundredPercent() {
        val actual = GenericPhrase().getReplyChance("hello world!")

        assertEquals(1f, actual)
    }
}