package com.github.mrbean355.zakbot.util

fun String.countOccurrences(word: String): Int =
    split(Regex("\\b")).count { it == word }