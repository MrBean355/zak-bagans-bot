package com.github.mrbean355.zakbot.phrases

internal class UnderstandPhraseTest : BasePhraseTest() {

    override val phrase = UnderstandPhrase()

    override val positiveCases = listOf(
        "We will never understand!",
        "I never understand Aaron",
    )

    override val negativeCases = listOf(
        "We will never understand (understand)!",
        "Ghost Adventures is the best"
    )
}