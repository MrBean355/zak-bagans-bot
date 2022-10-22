package com.github.mrbean355.zakbot.phrases

internal class SituationPhraseTest : PhraseTest() {

    override val creator = { SituationPhrase() }

    override val expectedPriority = 9

    override val replyChances = mapOf(
        "hello world!" to 0f,
        "it was a very intense situation" to 0.2f,
    )
}