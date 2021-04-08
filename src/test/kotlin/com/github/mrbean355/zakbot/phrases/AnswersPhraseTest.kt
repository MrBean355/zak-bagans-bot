package com.github.mrbean355.zakbot.phrases

internal class AnswersPhraseTest : BasePhraseTest() {

    override val phrase = AnswersPhrase()

    override val positiveCases = listOf(
        "zak always says: \"we want answers\", lol",
        "we want answers...",
        "intro: we  want    answers kek",
    )

    override val negativeCases = listOf(
        "answers answers answers",
        "zak always says: \"we want answers, answers...\", lol",
        "wwe want answersss",
    )
}