package com.github.mrbean355.zakbot.phrases

import org.springframework.stereotype.Component

@Component
class FeelingPhrase : Phrase {

    override val priority = 8

    override fun getReplyChance(message: String): Float {
        return if ("i feel" in message || "i'm feeling" in message) 0.25f else 0f
    }
}