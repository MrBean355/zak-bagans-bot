package com.github.mrbean355.zakbot.phrases

import org.springframework.stereotype.Component

@Component
class AnswersPhrase : Phrase {

    override val priority = 1

    override val responses = listOf(
        "... answers...",
    )

    override fun shouldReplyTo(message: String): Boolean {
        if (!message.contains(Regex("""we\s+want\s+answers"""))) {
            return false
        }
        val words = message.split(Regex("\\b")).count {
            it == "answers"
        }
        return words == 1
    }
}