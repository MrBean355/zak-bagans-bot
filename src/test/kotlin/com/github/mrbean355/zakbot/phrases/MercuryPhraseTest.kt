package com.github.mrbean355.zakbot.phrases

internal class MercuryPhraseTest : PhraseTest() {

    override val creator = { MercuryPhrase() }

    override val expectedPriority = 10

    override val replyChances = mapOf(
        "hello world!" to 0f,
        "did he drink mercury?!" to 0.25f,
    )
}