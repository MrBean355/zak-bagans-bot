package com.github.mrbean355.zakbot.phrases

internal class AnswersPhraseTest : PhraseTest() {

    override val creator = { AnswersPhrase() }

    override val expectedPriority = 1

    override val responseFileName = "phrases/answers.txt"

    override val replyChances = mapOf(
        "hello world!" to 0f,
        "we want answers..." to 0.5f,
        "we want answers... answers..." to 0f,
    )
}