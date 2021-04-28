package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.phrases.responses.ResponsePool
import org.springframework.stereotype.Component

@Component
class AaronPhrase : Phrase {

    override val priority = 3

    override val responses = ResponsePool.fromFile("phrases/aaron.txt")

    override fun getReplyChance(message: String): Float {
        return if ("aaron" in message) 0.25f else 0f
    }
}