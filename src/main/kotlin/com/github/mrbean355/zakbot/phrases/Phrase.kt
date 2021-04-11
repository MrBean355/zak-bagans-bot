package com.github.mrbean355.zakbot.phrases

interface Phrase {

    /** Order to check this [Phrase] relative to others. Higher means it will be checked before. */
    val priority: Int

    /** Possible messages to send if this [Phrase] is used. A random one will be picked. */
    val responses: List<String>

    /** Return a chance between 0 and 1, based on the [message], that this [Phrase] should be used. */
    fun getReplyChance(message: String): Float

}