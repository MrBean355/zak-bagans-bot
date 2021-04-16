package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.util.readResourceFileLines
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

internal class SituationPhraseTest {

    @Test
    internal fun testPriority() {
        assertEquals(9, SituationPhrase().priority)
    }

    @Test
    internal fun testResponses() {
        val responses = mockk<List<String>>()
        mockkStatic(::readResourceFileLines)
        every { readResourceFileLines(any()) } returns responses

        val actual = SituationPhrase().responses

        assertSame(responses, actual)
        verify {
            readResourceFileLines("phrases/situation.txt")
        }
    }

    @Test
    internal fun testGetReplyChance_DoesNotContainSituation_ReturnsZeroPercent() {
        val actual = SituationPhrase().getReplyChance("hello world!")

        assertEquals(0f, actual)
    }

    @Test
    internal fun testGetReplyChance_ContainsSituation_ReturnsFiftyPercent() {
        val actual = SituationPhrase().getReplyChance("it was a very intense situation")

        assertEquals(0.5f, actual)
    }
}