package com.github.mrbean355.zakbot.phrases

internal class FeelingPhraseTest : PhraseTest() {

    override val creator = { FeelingPhrase() }

    override val expectedPriority = 8

    override val responseFileName = "phrases/feeling.txt"

    override val replyChances = mapOf(
        "hello world!" to 0f,
        "i feel filled with rage" to 0.75f,
        "i'm feeling overwhelmed with sadness" to 0.75f,
    )
}