package com.github.mrbean355.zakbot

import kotlin.concurrent.fixedRateTimer

private val ZakPattern = """(?i)\bzak\b""".toRegex()

fun main() {
    fixedRateTimer(period = 5_000) {
        val lastChecked = getLastChecked()
        println(lastChecked)
        getRecentComments()
            .filter { it.created.time > lastChecked }
            .onFirst {
                setLastChecked(it.created.time)
            }
            .forEach { comment ->
                if (!comment.author.equals(BotUsername, true) && comment.body.contains(ZakPattern)) {
                    println(comment.body)
                }
            }
    }
}

private inline fun <T> Iterable<T>.onFirst(action: (T) -> Unit): Iterable<T> {
    firstOrNull()?.let(action)
    return this
}