package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.util.readResourceFileLines
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

internal class AaronPhraseTest {

    @Test
    internal fun testPriority() {
        assertEquals(2, AaronPhrase().priority)
    }

    @Test
    internal fun testResponses() {
        val responses = mockk<List<String>>()
        mockkStatic(::readResourceFileLines)
        every { readResourceFileLines(any()) } returns responses

        val actual = AaronPhrase().responses

        assertSame(responses, actual)
        verify {
            readResourceFileLines("phrases/aaron.txt")
        }
    }

    @Test
    internal fun testGetReplyChance_DoesNotContainAaron_ReturnsZeroPercent() {
        val actual = AaronPhrase().getReplyChance("hello world!")

        assertEquals(0f, actual)
    }

    @Test
    internal fun testGetReplyChance_ContainsPhrase_ContainsAaron_ReturnsCorrectPercent() {
        repeat(5) { iteration ->
            val builder = StringBuilder()
            repeat(iteration) {
                builder.append("aaron! ")
            }

            val actual = AaronPhrase().getReplyChance("go in alone! $builder")

            assertEquals(0.25f * iteration, actual, "Wrong chance for $iteration mentions")
        }
    }
}