package com.github.mrbean355.zakbot.phrases

import com.github.mrbean355.zakbot.util.readResourceFileLines
import org.springframework.stereotype.Component

@Component
class GenericPhrase : Phrase {

    override val priority = 0

    override val responses = readResourceFileLines("phrases/generic.txt")

    override fun getReplyChance(message: String) = 1f

}