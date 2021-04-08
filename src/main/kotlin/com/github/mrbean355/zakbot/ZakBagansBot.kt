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

    private val phrases = phrases.sortedByDescending { it.priority }.also { println(it) }

    @Scheduled(cron = "0 */5 * * * *")
    fun checkComments() {
        val lastChecked = cache.getLastChecked()
        val newComments = redditService.getLatestComments().filter { it.created.time > lastChecked }

        if (newComments.isNotEmpty()) {
            cache.setLastChecked(newComments.first().created.time)
        }

        newComments.forEach { comment ->
            if (comment.body.contains(MessagePattern)) {
                val phrase = phrases.find { it.shouldReplyTo(comment.body.toLowerCase()) }?.responses?.randomOrNull()
                notify(comment, phrase)
                // TODO: reply to comment.
            }
        }
    }

    private fun notify(comment: Comment, phrase: String?) {
        telegramBot.sendMessage(
            "Comment by ${comment.author}:\n" +
                    "${comment.body}\n" +
                    "\n" +
                    "Respond with:\n" +
                    "$phrase"
        )
    }
}