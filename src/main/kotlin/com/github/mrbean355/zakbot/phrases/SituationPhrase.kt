package com.github.mrbean355.zakbot.phrases

import org.springframework.stereotype.Component

@Component
class SituationPhrase : Phrase {

    override val priority = 9

    override fun getReplyChance(message: String): Float {
        return if ("situation" in message) 0.5f else 0f
    }
}