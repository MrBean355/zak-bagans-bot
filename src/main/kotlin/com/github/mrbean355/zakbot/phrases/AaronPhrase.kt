package com.github.mrbean355.zakbot.phrases

import org.springframework.stereotype.Component

@Component
class AaronPhrase : Phrase {

    override val priority = 3

    override fun getReplyChance(message: String): Float {
        return if ("aaron" in message) 0.1f else 0f
    }
}