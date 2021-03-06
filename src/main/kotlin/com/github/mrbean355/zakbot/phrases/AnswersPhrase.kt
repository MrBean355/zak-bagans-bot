package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.phrases.responses.ResponsePool
import com.github.mrbean355.zakbot.util.countOccurrences
import org.springframework.stereotype.Component

@Component
class AnswersPhrase : Phrase {

    override val priority = 1

    override val responses = ResponsePool.fromFile("phrases/answers.txt")

    override fun getReplyChance(message: String): Float {
        if (!message.contains(Regex("""\bwe\s+want\s+answers\b"""))) {
            return 0f
        }
        return if (message.countOccurrences("answers") == 1) 0.5f else 0f
    }
}