package com.github.mrbean355.zakbot.phrases

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal abstract class BasePhraseTest {
    abstract val phrase: Phrase
    abstract val positiveCases: List<String>
    abstract val negativeCases: List<String>

    @Test
    internal fun testPhrases_PositiveCases() {
        positiveCases.forEach {
            Assertions.assertTrue(phrase.shouldReplyTo(it), "Should return true for $it")
        }
    }

    @Test
    internal fun testPhrases_NegativeCases() {
        negativeCases.forEach {
            Assertions.assertFalse(phrase.shouldReplyTo(it), "Should return false for $it")
        }
    }
}