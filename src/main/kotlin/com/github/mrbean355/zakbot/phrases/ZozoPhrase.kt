package com.github.mrbean355.zakbot.phrases

import org.springframework.stereotype.Component

@Component
class ZozoPhrase : Phrase {

    override val priority = 11

    override fun getReplyChance(message: String): Float {
        return if ("zozo" in message) 0.25f else 0f
    }
}