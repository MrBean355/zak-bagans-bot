package com.github.mrbean355.zakbot

import com.github.mrbean355.zakbot.db.BotCache
import com.github.mrbean355.zakbot.db.repo.PhraseRepository
import com.github.mrbean355.zakbot.db.type
import com.github.mrbean355.zakbot.phrases.Phrase
import com.github.mrbean355.zakbot.substitutions.substitute
import com.github.mrbean355.zakbot.util.getString
import net.dean.jraw.models.Comment
import net.dean.jraw.models.PublicContribution
import net.dean.jraw.models.Submission
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class ZakBagansBot(
    private val redditService: RedditService,
    private val telegramNotifier: TelegramNotifier,
    private val botCache: BotCache,
    private val phraseRepository: PhraseRepository,
    phrases: List<Phrase>
) {

    private val phrases = phrases.sortedByDescending { it.priority }

    @Value("\${zakbot.replies.enabled:false}")
    private var sendReplies = false

    @Scheduled(fixedRate = 15 * 60 * 1000L)
    fun checkComments() {
        redditService.getSubmissionsSince(botCache.getLastPostTime()).apply {
            firstOrNull()?.created?.let(botCache::setLastPostTime)
            forEach(::processSubmission)
        }

        redditService.getCommentsSince(botCache.getLastCommentTime()).apply {
            firstOrNull()?.created?.let(botCache::setLastCommentTime)
            forEach(::processComment)
        }
    }

    private fun processSubmission(submission: Submission) {
        if (submission.isAuthorIgnored()) {
            return
        }

        val response = findPhrase(submission.title, submission.body)
            ?.substitute(submission)
            ?: return

        telegramNotifier.sendMessage(getString("telegram.new_submission", submission.author, submission.title, response))

        if (sendReplies) {
            redditService.replyToSubmission(submission, response)
        }
    }

    private fun processComment(comment: Comment) {
        if (comment.author == BotUsername ||
            comment.isAuthorIgnored() ||
            comment.getSubmission()?.isAuthorIgnored() == true
        ) {
            return
        }

        if (comment.shouldIgnoreAuthor()) {
            botCache.ignoreUser(comment.author, comment.fullName)
            telegramNotifier.sendMessage(getString("telegram.new_ignored_user", comment.author))
            if (sendReplies) {
                redditService.replyToComment(comment, getString("reddit.new_user_ignored"))
            }
            return
        }

        val response = findPhrase(comment.body)
            ?.substitute(comment)
            ?: return

        telegramNotifier.sendMessage(getString("telegram.new_comment", comment.author, comment.body, response))

        if (sendReplies) {
            redditService.replyToComment(comment, response)
        }
    }

    private fun findPhrase(vararg inputs: String?): String? {
        inputs.filterNotNull().forEach { input ->
            val phrase = phrases
                .find { Random.nextFloat() <= it.getReplyChance(input.lowercase()) }

            if (phrase != null) {
                val choices = phraseRepository.findByType(phrase.type())
                val lowestUsage = choices.minOf { it.usages }

                return choices.filter { it.usages == lowestUsage }
                    .random()
                    .let {
                        phraseRepository.save(it.copy(usages = it.usages + 1))
                        it.content
                    }
            }
        }
        return null
    }

    private fun PublicContribution<*>.isAuthorIgnored(): Boolean =
        botCache.isUserIgnored(author)

    private fun Comment.getSubmission(): Submission? =
        redditService.getCommentSubmission(this)

    private fun Comment.shouldIgnoreAuthor(): Boolean {
        val isBadBot = body.filter { it.isLetter() }.equals("badbot", ignoreCase = true)
        return if (isBadBot) {
            redditService.findParentComment(this)
                ?.author == BotUsername
        } else false
    }
}