package com.github.mrbean355.zakbot

import com.github.mrbean355.zakbot.phrases.Phrase
import net.dean.jraw.models.Comment
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class ZakBagansBot(
    private val redditService: RedditService,
    private val telegramNotifier: TelegramNotifier,
    private val cache: Cache,
    phrases: List<Phrase>
) {

    private val phrases = phrases.sortedByDescending { it.priority }

    @Value("\${zakbot.replies.enabled:false}")
    private var sendReplies = false

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
            .forEach(this::processComment)
    }

    private fun processComment(comment: Comment) {
        val message = comment.body.toLowerCase()
        val response = phrases
            .find { Random.nextFloat() <= it.getReplyChance(message) }
            ?.responses?.randomOrNull()
            ?: return

        notify(comment, response)
        if (sendReplies) {
            redditService.replyToComment(comment, response)
        }
    }

    private fun notify(comment: Comment, response: String) {
        telegramNotifier.sendMessage(
            "Comment by ${comment.author}:\n" +
                    "${comment.body}\n" +
                    "\n" +
                    "Respond with:\n" +
                    response
        )
    }
}