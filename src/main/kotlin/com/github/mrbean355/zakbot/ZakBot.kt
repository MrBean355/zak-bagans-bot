package com.github.mrbean355.zakbot

import net.dean.jraw.models.Comment
import kotlin.concurrent.fixedRateTimer

class ZakBot {
    private val cache = Cache
    private val telegram = TelegramBot

    fun start() {
        telegram.sendMessage("Starting up...")
        fixedRateTimer(period = 5_000) {
            val lastChecked = cache.getLastChecked()
            val newComments = getRecentComments().filter { it.created.time > lastChecked }

            if (newComments.isNotEmpty()) {
                cache.setLastChecked(newComments.first().created.time)
            }

            newComments.forEach { comment ->
                if (!comment.author.equals(BotUsername, true) && comment.body.contains(ZakPattern)) {
                    notify(comment)
                }
            }
        }
    }

    private fun notify(comment: Comment) {
        telegram.sendMessage(
            """
                Comment by ${comment.author}:
                
                ${comment.body}
                
                https://www.reddit.com/r/GhostAdventures/comments/${comment.submissionFullName.substringAfter('_')}
            """.trimIndent()
        )
    }
}

private val ZakPattern = """(?i)\bzak\b""".toRegex()