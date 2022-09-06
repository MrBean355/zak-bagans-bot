package com.github.mrbean355.zakbot.phrases

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal abstract class PhraseTest {
    protected abstract val creator: () -> Phrase
    protected abstract val expectedPriority: Int
    protected abstract val replyChances: Map<String, Float>

    @Test
    internal fun testPriority() {
        assertEquals(expectedPriority, creator().priority)
    }

    @Test
    internal fun testReplyChance() {
        val phrase = creator()

        replyChances.forEach { (message, chance) ->
            assertEquals(chance, phrase.getReplyChance(message), "Wrong chance for message '$message'")
        }
    }
}