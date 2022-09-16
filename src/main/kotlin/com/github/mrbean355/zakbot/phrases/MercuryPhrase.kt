package com.github.mrbean355.zakbot.phrases

import org.springframework.stereotype.Component

@Component
class MercuryPhrase : Phrase {

    override val priority = 10

    override fun getReplyChance(message: String): Float {
        return if ("mercury" in message) 0.75f else 0f
    }
}