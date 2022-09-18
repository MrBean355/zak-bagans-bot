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
        checkForBotMention(submission)

        if (submission.isAuthorIgnored()) {
            return
        }

        val response = findPhrase(submission.title, submission.selfText)
            ?.substitute(submission)
            ?: return

        telegramNotifier.sendMessage(getString("telegram.new_submission", submission.author, submission.title, response))

        if (sendReplies) {
            redditService.replyToSubmission(submission, response)
        }
    }

    /*
     * Ignore comments that:
     * - are sent by the bot, or
     * - are sent by an ignored user, or
     * - are replies to a comment sent by the bot, or
     * - belong to a submission created by an ignored user.
     */
    private fun processComment(comment: Comment) {
        checkForBotMention(comment)

        if (comment.author == BotUsername ||
            comment.isAuthorIgnored() ||
            redditService.getCommentSubmission(comment)?.isAuthorIgnored() == true
        ) {
            return
        }

        if (redditService.findParentComment(comment)?.author == BotUsername) {
            if (comment.mentionsBadBot()) {
                botCache.ignoreUser(comment.author, comment.fullName)
                telegramNotifier.sendMessage(getString("telegram.new_ignored_user", comment.author))
                if (sendReplies) {
                    redditService.replyToComment(comment, getString("reddit.new_user_ignored"))
                }
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

    /** Send a Telegram notification when someone mentions something 'bot' related. */
    private fun checkForBotMention(contribution: PublicContribution<*>) {
        val text = when (contribution) {
            is Submission -> contribution.title + contribution.selfText.orEmpty()
            is Comment -> contribution.body
            else -> return
        }.lowercase()

        if (text.contains(Regex("\\bbot\\b")) ||
            "zakbot" in text ||
            "zacbot" in text ||
            "baganbot" in text ||
            "bagansbot" in text
        ) {
            if (contribution is Submission) {
                telegramNotifier.sendMessage(getString("telegram.new_bot_mention_submission", contribution.author, contribution.title))
            } else {
                telegramNotifier.sendMessage(getString("telegram.new_bot_mention_comment", contribution.author, contribution.body))
            }
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

    private fun Comment.mentionsBadBot(): Boolean {
        return body.filter(Char::isLetter).equals("badbot", ignoreCase = true)
    }
}