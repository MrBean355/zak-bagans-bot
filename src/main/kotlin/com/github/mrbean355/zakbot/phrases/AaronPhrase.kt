package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.util.countOccurrences
import com.github.mrbean355.zakbot.util.readResourceFileLines
import org.springframework.stereotype.Component

@Component
class AaronPhrase : Phrase {

    override val priority = 2

    override val responses = readResourceFileLines("phrases/aaron.txt")

    override fun getReplyChance(message: String): Float {
        return 0.25f * message.countOccurrences("aaron")
    }
}