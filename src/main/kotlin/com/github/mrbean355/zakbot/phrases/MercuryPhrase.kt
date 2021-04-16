package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.util.readResourceFileLines
import org.springframework.stereotype.Component

@Component
class MercuryPhrase : Phrase {

    override val priority = 10

    override val responses = readResourceFileLines("phrases/mercury.txt")

    override fun getReplyChance(message: String): Float {
        return if ("mercury" in message) 1f else 0f
    }
}