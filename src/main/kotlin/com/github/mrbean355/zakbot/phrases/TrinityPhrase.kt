package com.github.mrbean355.zakbot.phrases

import org.springframework.stereotype.Component

/** Reply to messages matching this pattern. */
private val DigitRegex = """\b3\b""".toRegex()

@Component
class TrinityPhrase : Phrase {

    override val priority = 7

    override fun getReplyChance(message: String): Float {
        return if (message.contains(DigitRegex) || message.contains("three", ignoreCase = true)) 0.1f else 0f
    }
}