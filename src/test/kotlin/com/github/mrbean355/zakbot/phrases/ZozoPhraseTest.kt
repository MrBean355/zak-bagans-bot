package com.github.mrbean355.zakbot.phrases

internal class ZozoPhraseTest : PhraseTest() {

    override val creator = { ZozoPhrase() }

    override val expectedPriority = 11

    override val responseFileName = "phrases/zozo.txt"

    override val replyChances = mapOf(
        "hello world!" to 0f,
        "he called zozo!" to 0.75f,
    )
}