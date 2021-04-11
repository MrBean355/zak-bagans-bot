package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.util.countOccurrences
import com.github.mrbean355.zakbot.util.readResourceFileLines
import org.springframework.stereotype.Component

@Component
class UnderstandPhrase : Phrase {

    override val priority = 1

    override val responses = readResourceFileLines("phrases/understand.txt")

    override fun getReplyChance(message: String): Float {
        if (!message.contains(Regex("""\bunderstand\b"""))) {
            return 0f
        }
        return if (message.countOccurrences("understand") == 1) 0.5f else 0f
    }
}