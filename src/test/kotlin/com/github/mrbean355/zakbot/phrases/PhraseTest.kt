package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.phrases.responses.ResponsePool
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal abstract class PhraseTest {
    protected abstract val creator: () -> Phrase
    protected abstract val expectedPriority: Int
    protected abstract val responseFileName: String
    protected abstract val replyChances: Map<String, Float>

    @Test
    internal fun testPriority() {
        assertEquals(expectedPriority, creator().priority)
    }

    @Test
    internal fun testResponses() {
        val pool = mockk<ResponsePool>()
        mockkObject(ResponsePool)
        every { ResponsePool.fromFile(any()) } returns pool

        Assertions.assertSame(pool, creator().responses)
        verify { ResponsePool.fromFile(responseFileName) }

        unmockkObject(ResponsePool)
    }

    @Test
    internal fun testReplyChance() {
        val phrase = creator()

        replyChances.forEach { (message, chance) ->
            assertEquals(chance, phrase.getReplyChance(message), "Wrong chance for message '$message'")
        }
    }
}