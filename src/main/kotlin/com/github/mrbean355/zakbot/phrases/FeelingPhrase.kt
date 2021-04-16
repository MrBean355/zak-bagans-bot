package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.util.readResourceFileLines
import org.springframework.stereotype.Component

@Component
class FeelingPhrase : Phrase {

    override val priority = 8

    override val responses = readResourceFileLines("phrases/feeling.txt")

    override fun getReplyChance(message: String): Float {
        return if ("i feel" in message || "i'm feeling" in message) 0.5f else 0f
    }
}