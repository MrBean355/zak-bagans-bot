package com.github.mrbean355.zakbot

import com.github.mrbean355.zakbot.phrases.Phrase
import net.dean.jraw.models.Comment
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** Reply to messages matching this pattern. */
private val MessagePattern = """(?i)\b(zak|bagans)\b""".toRegex()

@Component
class ZakBagansBot(
    private val redditService: RedditService,
    private val telegramBot: TelegramBot,
    private val cache: Cache,
    phrases: List<Phrase>
) {

    private val phrases = phrases.sortedByDescending { it.priority }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    fun checkComments() {
        val lastChecked = cache.getLastChecked()
        val newComments = redditService.getLatestComments().filter {
            it.created.time > lastChecked
        }

        if (newComments.isNotEmpty()) {
            cache.setLastChecked(newComments.first().created.time)
        }

        newComments
            .filter { it.author != BotUsername }
            .filter { it.body.contains(MessagePattern) }
            .forEach(this::processComment)
    }

    private fun processComment(comment: Comment) {
        val response = phrases.find { it.shouldReplyTo(comment.body.toLowerCase()) }
            ?.responses?.randomOrNull()
            ?: return

        notify(comment, response)
        redditService.replyToComment(comment, response)
    }

    private fun notify(comment: Comment, response: String) {
        telegramBot.sendMessage(
            "Comment by ${comment.author}:\n" +
                    "${comment.body}\n" +
                    "\n" +
                    "Respond with:\n" +
                    response
        )
    }
}