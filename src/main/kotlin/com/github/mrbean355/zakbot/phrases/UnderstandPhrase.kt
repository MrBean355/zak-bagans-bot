package com.github.mrbean355.zakbot.phrases

import org.springframework.stereotype.Component

@Component
class UnderstandPhrase : Phrase {

    override val priority = 1

    override val responses = listOf(
        "... understand...",
    )

    override fun shouldReplyTo(message: String): Boolean {
        if (!message.contains(Regex("""\bunderstand\b"""))) {
            return false
        }
        val words = message.split(Regex("\\b")).count {
            it == "understand"
        }
        return words == 1
    }
}