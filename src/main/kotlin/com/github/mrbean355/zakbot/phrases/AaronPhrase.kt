package com.github.mrbean355.zakbot.phrases

import org.springframework.stereotype.Component

/** Reply to messages matching this pattern. */
private val AaronRegex = """(?i)\b(aaron)\b""".toRegex()

@Component
class AaronPhrase : Phrase {

    override val priority = 3

    override fun getReplyChance(message: String): Float {
        return if (message.contains(AaronRegex)) 0.1f else 0f
    }
}