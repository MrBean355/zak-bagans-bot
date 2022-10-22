package com.github.mrbean355.zakbot.phrases

internal class GenericPhraseTest : PhraseTest() {

    override val creator = { GenericPhrase() }

    override val expectedPriority = 0

    override val replyChances = mapOf(
        "hello zak!" to 0.15f,
        "hello mr bagans!" to 0.15f,
        "hello aaron!" to 0f,
    )
}