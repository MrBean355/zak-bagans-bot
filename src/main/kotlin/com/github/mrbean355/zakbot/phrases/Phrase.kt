package com.github.mrbean355.zakbot.phrases

sealed interface Phrase {

    /** Order to check this [Phrase] relative to others. Higher means it will be checked before. */
    val priority: Int

    /** Return a chance between 0 and 1, based on the [message], that this [Phrase] should be used. */
    fun getReplyChance(message: String): Float

}