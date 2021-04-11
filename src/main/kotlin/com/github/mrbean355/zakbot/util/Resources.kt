package com.github.mrbean355.zakbot.util

import com.github.mrbean355.zakbot.phrases.Phrase
import java.io.File

fun readResourceFileLines(filePath: String): List<String> {
    val url = Phrase::class.java.classLoader.getResource(filePath)
    require(url != null) { "Failed to load resource: $filePath" }

    return File(url.file).readLines().asSequence()
        .map { it.substringBefore('#').trim() }
        .filter { it.isNotEmpty() }
        .toList()
}