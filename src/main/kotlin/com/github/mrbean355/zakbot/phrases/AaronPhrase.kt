package com.github.mrbean355.zakbot.phrases

import org.springframework.stereotype.Component

@Component
class AaronPhrase : Phrase {

    override val priority = 2

    override val responses = loadPhrases("aaron.txt")

    override fun shouldReplyTo(message: String): Boolean {
        return "aaron" in message
    }
}