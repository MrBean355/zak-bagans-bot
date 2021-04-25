package com.github.mrbean355.zakbot.phrases

internal class MercuryPhraseTest : PhraseTest() {

    override val creator = { MercuryPhrase() }

    override val expectedPriority = 10

    override val responseFileName = "phrases/mercury.txt"

    override val replyChances = mapOf(
        "hello world!" to 0f,
        "did he drink mercury?!" to 0.75f,
    )
}