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

private val BotRegex = """\bbot\b""".toRegex()
private val UrlRegex = """[-a-zA-Z0-9@:%._+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)""".toRegex()

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

    @Scheduled(cron = "@midnight")
    fun updateBotFlair() {
        val options = redditService.getFlairOptions()
        if (options.isNotEmpty()) {
            redditService.setBotFlair(options.random())
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

        telegramNotifier.sendMessage(getString("telegram.new_submission", submission.title, response, submission.url))

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
                telegramNotifier.sendMessage(getString("telegram.new_ignored_user", comment.author, comment.url))
                if (sendReplies) {
                    redditService.replyToComment(comment, getString("reddit.new_user_ignored"))
                }
            }
            return
        }

        val response = findPhrase(comment.body)
            ?.substitute(comment)
            ?: return

        telegramNotifier.sendMessage(getString("telegram.new_comment", comment.body, response, comment.url))

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

        if (text.contains(BotRegex) ||
            "zakbot" in text ||
            "zacbot" in text ||
            "baganbot" in text ||
            "bagansbot" in text
        ) {
            if (contribution is Submission) {
                telegramNotifier.sendMessage(getString("telegram.new_bot_mention_submission", contribution.title, contribution.url))
            } else if (contribution is Comment) {
                telegramNotifier.sendMessage(getString("telegram.new_bot_mention_comment", contribution.body, contribution.url))
            }
        }
    }

    private fun findPhrase(vararg inputs: String?): String? {
        inputs.filterNotNull().forEach { input ->
            val text = input.lowercase().replace(UrlRegex, "")
            val phrase = phrases.find { Random.nextFloat() <= it.getReplyChance(text) }

            if (phrase != null) {
                val choices = phraseRepository.findByType(phrase.type())
                val lowestUsage = choices.minOf { it.usages }

                return choices.filter { it.usages == lowestUsage }
                    .random()
                    .let { entity ->
                        phraseRepository.save(entity.copy(usages = entity.usages + 1))
                        if (entity.source != null) {
                            getString("reddit.quote_source_prefix", entity.content, entity.source.escapeParentheses())
                        } else {
                            entity.content
                        }
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

    private fun String.escapeParentheses(): String {
        return replace("(", "\\(")
            .replace(")", "\\)")
    }
}