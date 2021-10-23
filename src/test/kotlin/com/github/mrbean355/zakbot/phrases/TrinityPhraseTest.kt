package com.github.mrbean355.zakbot.phrases

internal class TrinityPhraseTest : PhraseTest() {

    override val creator = { TrinityPhrase() }

    override val expectedPriority = 7

    override val responseFileName = "phrases/trinity.txt"

    override val replyChances = mapOf(
        "hello world!" to 0f,
        "I love the number 3!" to 0.33f,
        "but not the number 33.." to 0f,
        "Three is also pretty cool" to 0.33f,
    )
}