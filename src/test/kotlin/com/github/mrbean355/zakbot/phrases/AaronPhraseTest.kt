package com.github.mrbean355.zakbot.phrases

internal class AaronPhraseTest : PhraseTest() {

    override val creator = { AaronPhrase() }

    override val expectedPriority = 3

    override val replyChances = mapOf(
        "hello world!" to 0f,
        "hello aaron!" to 0.25f,
    )
}