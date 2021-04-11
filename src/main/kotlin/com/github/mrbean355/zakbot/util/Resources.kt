package com.github.mrbean355.zakbot.util

import com.github.mrbean355.zakbot.phrases.Phrase

fun readResourceFileLines(filePath: String): List<String> {
    val stream = Phrase::class.java.classLoader.getResourceAsStream(filePath)
    require(stream != null) { "Failed to load resource: $filePath" }

    return stream.use { it.readBytes().decodeToString() }
        .split("\n")
        .map { it.substringBefore('#').trim() }
        .filter { it.isNotEmpty() }
}