package com.github.mrbean355.zakbot.phrases

import org.springframework.stereotype.Component

@Component
class BasePhrase : Phrase {

    override val priority = 0

    override val responses = listOf(
        // Lines from the intro:
        "There are things in this world we will never fully understand... understand...",
        "We want answers... answers...",
        "We have worked years to build our credibility... our reputation...",
        "Working alongside the most renowned professionals in the field.",
        "Capturing ground-breaking proof of the paranormal.",
        // "I can't give you an explanation", // should we include this?
        "This is our evidence... our ghost adventures.",

        // Generic lines:
        "BRO!",
        "DUDE!",
        "That's a class-A EVP right there!",
        "Bro, are you serious?!",
        "SHHHHHH!!! Be quiet! Listen!",
        "Dude is it getting cold in here?!",
        "Could there be a portal here?",
        "I don't like bullies! You want to pick on someone? Pick on me!",
        "I'm sending Aaron in alone to investigate.",
    )

    override fun shouldReplyTo(message: String) = true

}