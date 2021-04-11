package com.github.mrbean355.zakbot.phrases

import java.io.File

interface Phrase {

    /** Order to check this [Phrase] relative to others. Higher means earlier. */
    val priority: Int

    /** Possible phrases to say if [shouldReplyTo] returns true. */
    val responses: List<String>

    /** Analyse the [message] and return true if a phrase should be sent. */
    fun shouldReplyTo(message: String): Boolean

}

fun loadPhrases(fileName: String): List<String> {
    val url = Phrase::class.java.classLoader.getResource("phrases/$fileName")
    require(url != null) { "Failed to load resource: phrases/$fileName" }

    return File(url.file).readLines().asSequence()
        .map { it.substringBefore('#').trim() }
        .filter { it.isNotEmpty() }
        .toList()
}