package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.util.readResourceFileLines
import org.springframework.stereotype.Component

/** Reply to messages matching this pattern. */
private val ZakRegex = """(?i)\b(zak|bagans)\b""".toRegex()

@Component
class GenericPhrase : Phrase {

    override val priority = 0

    override val responses = readResourceFileLines("phrases/generic.txt")

    override fun getReplyChance(message: String): Float {
        return if (message.contains(ZakRegex)) 1f else 0f
    }
}