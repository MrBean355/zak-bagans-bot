package com.github.mrbean355.zakbot.phrases

import org.springframework.stereotype.Component

@Component
class AnswersPhrase : Phrase {

    override val priority = 1

    override val responses = loadPhrases("answers.txt")

    override fun shouldReplyTo(message: String): Boolean {
        if (!message.contains(Regex("""\bwe\s+want\s+answers\b"""))) {
            return false
        }
        val words = message.split(Regex("\\b")).count {
            it == "answers"
        }
        return words == 1
    }
}