package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.util.countOccurrences
import org.springframework.stereotype.Component

@Component
class UnderstandPhrase : Phrase {

    override val priority = 2

    override fun getReplyChance(message: String): Float {
        if (message.length > 100 || !message.contains(Regex("""\bunderstand\b"""))) {
            return 0f
        }
        return if (message.countOccurrences("understand") == 1) 0.5f else 0f
    }
}