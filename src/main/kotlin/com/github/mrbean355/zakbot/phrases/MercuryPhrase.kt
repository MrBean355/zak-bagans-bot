package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.phrases.responses.ResponsePool
import org.springframework.stereotype.Component

@Component
class MercuryPhrase : Phrase {

    override val priority = 10

    override val responses = ResponsePool.fromFile("phrases/mercury.txt")

    override fun getReplyChance(message: String): Float {
        return if ("mercury" in message) 0.75f else 0f
    }
}