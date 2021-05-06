package com.github.mrbean355.zakbot

import com.github.mrbean355.zakbot.phrases.Phrase
import com.github.mrbean355.zakbot.substitutions.substitute
import net.dean.jraw.models.Comment
import net.dean.jraw.models.Submission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.Date
import javax.annotation.PostConstruct
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

    @PostConstruct
    fun onPostConstruct() {
        val logger = LoggerFactory.getLogger(ZakBagansBot::class.java)

        logger.info("Order of phrases:")
        phrases.forEachIndexed { index, phrase ->
            logger.info("$index: ${phrase::class.java.simpleName}")
        }

        val totalResponses = phrases.fold(0) { acc, phrase ->
            acc.plus(phrase.responses.totalSize)
        }
        telegramNotifier.sendMessage(
            "Zak Bot v$AppVersion started up!\n" +
                    "Phrases: ${phrases.size}\n" +
                    "Responses: $totalResponses"
        )
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    fun checkComments() {
        redditService.getSubmissionsSince(Date(cache.getLastPost())).apply {
            firstOrNull()?.created?.time?.let(cache::setLastPost)
            forEach(::processSubmission)
        }

        redditService.getCommentsSince(Date(cache.getLastComment())).apply {
            firstOrNull()?.created?.time?.let(cache::setLastComment)
            forEach(::processComment)
        }
    }

    private fun processSubmission(submission: Submission) {
        val response = findPhrase(submission.title, submission.body)
            ?.substitute(submission)
            ?: return

        telegramNotifier.sendMessage(
            "New submission:\n" +
                    "${submission.author}: ${submission.title}\n" +
                    "Response: $response"
        )
        if (sendReplies) {
            redditService.replyToSubmission(submission, response)
        }
    }

    private fun processComment(comment: Comment) {
        if (comment.author == BotUsername) {
            return
        }
        val response = findPhrase(comment.body)
            ?.substitute(comment)
            ?: return

        telegramNotifier.sendMessage(
            "New comment:\n" +
                    "${comment.author}: ${comment.body}\n" +
                    "Response: $response"
        )
        if (sendReplies) {
            redditService.replyToComment(comment, response)
        }
    }

    private fun findPhrase(vararg inputs: String?): String? {
        inputs.filterNotNull().forEach { input ->
            val phrase = phrases
                .find { Random.nextFloat() <= it.getReplyChance(input.lowercase()) }
                ?.responses?.take()

            if (phrase != null) {
                return phrase
            }
        }
        return null
    }
}