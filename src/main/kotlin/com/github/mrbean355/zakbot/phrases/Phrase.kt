package com.github.mrbean355.zakbot.phrases

interface Phrase {

    /** Order to check this [Phrase] relative to others. Higher means earlier. */
    val priority: Int

    /** Possible phrases to say if [shouldReplyTo] returns true. */
    val responses: List<String>

    /** Analyse the [message] and return true if a phrase should be sent. */
    fun shouldReplyTo(message: String): Boolean

}