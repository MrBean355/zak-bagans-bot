package com.github.mrbean355.zakbot.phrases

internal class UnderstandPhraseTest : PhraseTest() {

    override val creator = { UnderstandPhrase() }

    override val expectedPriority = 2

    override val responseFileName = "phrases/understand.txt"

    override val replyChances = mapOf(
        "hello world!" to 0f,
        "we will never understand!" to 0.5f,
        "we will never understand... understand!" to 0f,
    )
}