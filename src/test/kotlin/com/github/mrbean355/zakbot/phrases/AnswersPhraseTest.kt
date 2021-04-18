package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.util.readResourceFileLines
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

internal class AnswersPhraseTest {

    @Test
    internal fun testPriority() {
        assertEquals(1, AnswersPhrase().priority)
    }

    @Test
    internal fun testResponses() {
        val responses = mockk<List<String>>()
        mockkStatic(::readResourceFileLines)
        every { readResourceFileLines(any()) } returns responses

        val actual = AnswersPhrase().responses

        assertSame(responses, actual)
        verify {
            readResourceFileLines("phrases/answers.txt")
        }
    }

    @Test
    internal fun testGetReplyChance_DoesNotContainPhrase_ReturnsZeroPercent() {
        val actual = AnswersPhrase().getReplyChance("hello world!")

        assertEquals(0f, actual)
    }

    @Test
    internal fun testGetReplyChance_ContainsPhrase_ContainsAnswersOnce_ReturnsOneHundredPercent() {
        val actual = AnswersPhrase().getReplyChance("we want answers...")

        assertEquals(0.75f, actual)
    }

    @Test
    internal fun testGetReplyChance_ContainsPhrase_ContainsAnswersMultipleTimes_ReturnsZeroPercent() {
        val actual = AnswersPhrase().getReplyChance("we want answers... answers...")

        assertEquals(0f, actual)
    }
}