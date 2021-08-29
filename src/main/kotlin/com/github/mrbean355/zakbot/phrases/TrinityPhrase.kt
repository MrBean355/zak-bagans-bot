package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.phrases.responses.ResponsePool
import org.springframework.stereotype.Component

/** Reply to messages matching this pattern. */
private val DigitRegex = """\b3\b""".toRegex()

@Component
class TrinityPhrase : Phrase {

    override val priority = 7

    override val responses = ResponsePool.fromFile("phrases/trinity.txt")

    override fun getReplyChance(message: String): Float {
        return if (message.contains(DigitRegex) || message.contains("three", ignoreCase = true)) 0.33f else 0f
    }
}