package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.phrases.responses.ResponsePool
import org.springframework.stereotype.Component

@Component
class ZozoPhrase : Phrase {

    override val priority = 11

    override val responses = ResponsePool.fromFile("phrases/zozo.txt")

    override fun getReplyChance(message: String): Float {
        return if ("zozo" in message) 0.75f else 0f
    }
}