package com.github.mrbean355.zakbot.phrases

import org.springframework.stereotype.Component

@Component
class BasePhrase : Phrase {

    override val priority = 0

    override val responses = loadPhrases("generic.txt")

    override fun shouldReplyTo(message: String) = true

}