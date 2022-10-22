package com.github.mrbean355.zakbot.phrases

internal class UnderstandPhraseTest : PhraseTest() {

    override val creator = { UnderstandPhrase() }

    override val expectedPriority = 2

    override val replyChances = mapOf(
        "hello world!" to 0f,
        "we will never understand!" to 0.2f,
        "we will never understand... understand!" to 0f,
        "we will never understand! blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah" to 0.2f,
        "we will never understand! blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah!" to 0f,
    )
}