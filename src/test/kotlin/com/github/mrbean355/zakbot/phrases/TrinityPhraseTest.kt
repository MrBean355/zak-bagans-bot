package com.github.mrbean355.zakbot.phrases

internal class TrinityPhraseTest : PhraseTest() {

    override val creator = { TrinityPhrase() }

    override val expectedPriority = 7

    override val replyChances = mapOf(
        "hello world!" to 0f,
        "I love the number 3!" to 0.1f,
        "but not the number 33.." to 0f,
        "Three is also pretty cool" to 0.1f,
    )
}