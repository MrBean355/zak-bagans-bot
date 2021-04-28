package com.github.mrbean355.zakbot.phrases.responses

import com.github.mrbean355.zakbot.util.readResourceFileLines

/**
 * Data structure that returns a random element from [responses].
 * Each element can only be returned once, until there are none left.
 * At this point, the elements will get shuffled and become available again.
 */
class ResponsePool(
    private val responses: List<String>
) {

    private val available = mutableListOf<String>()

    val currentSize: Int get() = available.size
    val totalSize: Int get() = responses.size

    init {
        require(responses.isNotEmpty()) { "Responses must not be empty" }
        reset()
    }

    fun take(): String = synchronized(this) {
        if (available.isEmpty()) {
            reset()
        }
        available.removeFirst()
    }

    private fun reset() {
        available += responses.shuffled()
    }

    companion object {
        fun fromFile(filePath: String): ResponsePool = ResponsePool(readResourceFileLines(filePath))
    }
}