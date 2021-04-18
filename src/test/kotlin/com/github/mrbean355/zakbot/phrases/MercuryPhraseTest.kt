package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.util.readResourceFileLines
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

internal class MercuryPhraseTest {

    @Test
    internal fun testPriority() {
        assertEquals(10, MercuryPhrase().priority)
    }

    @Test
    internal fun testResponses() {
        val responses = mockk<List<String>>()
        mockkStatic(::readResourceFileLines)
        every { readResourceFileLines(any()) } returns responses

        val actual = MercuryPhrase().responses

        assertSame(responses, actual)
        verify {
            readResourceFileLines("phrases/mercury.txt")
        }
    }

    @Test
    internal fun testGetReplyChance_DoesNotContainMercury_ReturnsZeroPercent() {
        val actual = MercuryPhrase().getReplyChance("hello world!")

        assertEquals(0f, actual)
    }

    @Test
    internal fun testGetReplyChance_ContainsMercury_ReturnsOneHundredPercent() {
        val actual = MercuryPhrase().getReplyChance("did he drink mercury?!")

        assertEquals(0.75f, actual)
    }
}