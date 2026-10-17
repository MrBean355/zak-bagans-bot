package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.AuthorUsername
import com.github.mrbean355.zakbot.BotUsername
import com.github.mrbean355.zakbot.TelegramNotifier
import com.github.mrbean355.zakbot.db.BotCache
import com.github.mrbean355.zakbot.db.PhraseType
import com.github.mrbean355.zakbot.reddit.model.Comment
import com.github.mrbean355.zakbot.reddit.model.Contribution
import com.github.mrbean355.zakbot.reddit.model.Submission
import com.github.mrbean355.zakbot.util.getString
import org.springframework.stereotype.Service
import kotlin.random.Random

private const val SUBSTITUTION_AUTHOR_NAME = "{author}"
private const val GOOD_BOT_REPLY_CHANCE = 0.1f
private val GOOD_BOT_PHRASES = setOf("goodbot", "greatbot", "bestbot")

private val BOT_REGEX = """\bbot\b""".toRegex()

@Service
class ContributionService(
    private val redditService: RedditService,
    private val phraseService: PhraseService,
    private val commandService: CommandService,
    private val botCache: BotCache,
    private val telegramNotifier: TelegramNotifier,
    private val randomRoll: () -> Float = { Random.nextFloat() },
) {

    /**
     * Ignores submissions that are:
     * - sent by the bot's author, or
     * - sent by an ignored user.
     */
    fun processSubmission(submission: Submission) {
        if (submission.author == AuthorUsername) {
            return
        }

        checkForBotMention(submission)

        if (submission.isAuthorIgnored()) {
            return
        }

        val response = phraseService.findPhrase(submission)
            ?.substitute(submission)
            ?: return

        redditService.replyToSubmission(submission, response)
        telegramNotifier.sendMessage(getString("telegram.new_submission", submission.title, response, submission.url))
    }

    /**
     * Ignore comments that are:
     * - sent by the bot's author that aren't commands, or
     * - sent by the bot itself, or
     * - sent by an ignored user, or
     * - replies to an ignored submission (via ignore command), or
     * - replies to a submission created by an ignored user, or
     * - replies to a comment sent by the bot.
     */
    fun processComment(comment: Comment) {
        if (comment.author == AuthorUsername) {
            commandService.processAuthorCommand(comment)
            return
        }

        checkForBotMention(comment)

        if (comment.author == BotUsername ||
            comment.isAuthorIgnored() ||
            botCache.isSubmissionIgnored(comment.submissionFullName) ||
            redditService.getCommentSubmission(comment)?.isAuthorIgnored() == true
        ) {
            return
        }

        if (redditService.findParentComment(comment)?.author == BotUsername) {
            if (comment.body.isBadBot()) {
                botCache.ignoreUser(comment.author, comment.fullName)
                redditService.replyToComment(comment, getString("reddit.new_user_ignored"))
                telegramNotifier.sendMessage(getString("telegram.new_ignored_user", comment.author, comment.url))
            } else if (comment.body.isGoodBot() && randomRoll() <= GOOD_BOT_REPLY_CHANCE) {
                val phrase = phraseService.getPhrase(PhraseType.GoodBot)
                    ?.substitute(comment)
                if (phrase != null) {
                    val response = getString("reddit.good_bot_footer", phrase)
                    redditService.replyToComment(comment, response)
                    telegramNotifier.sendMessage(getString("telegram.good_bot_praise", comment.author, response, comment.url))
                }
            }
            return
        }

        val response = phraseService.findPhrase(comment)
            ?.substitute(comment)
            ?: return

        redditService.replyToComment(comment, response)
        telegramNotifier.sendMessage(getString("telegram.new_comment", comment.body, response, comment.url))
    }

    /**
     * Send a Telegram notification when someone mentions something 'bot' related.
     */
    private fun checkForBotMention(contribution: Contribution) {
        val text = when (contribution) {
            is Submission -> contribution.title + contribution.selfText.orEmpty()
            is Comment -> contribution.body
        }.lowercase().trim()

        if (text.isGoodBot()) {
            return
        }

        if (text.contains(BOT_REGEX) ||
            "zakbot" in text ||
            "zacbot" in text ||
            "baganbot" in text ||
            "bagansbot" in text
        ) {
            when (contribution) {
                is Submission -> telegramNotifier.sendMessage(
                    getString(
                        "telegram.new_bot_mention_submission",
                        contribution.title,
                        contribution.url
                    )
                )

                is Comment -> telegramNotifier.sendMessage(
                    getString(
                        "telegram.new_bot_mention_comment",
                        contribution.body,
                        contribution.url
                    )
                )
            }
        }
    }

    private fun Contribution.isAuthorIgnored(): Boolean {
        return botCache.isUserIgnored(author)
    }

    private fun String.isGoodBot(): Boolean {
        return filter(Char::isLetter).lowercase() in GOOD_BOT_PHRASES
    }

    private fun String.isBadBot(): Boolean {
        return filter(Char::isLetter).equals("badbot", ignoreCase = true)
    }

    private fun String.substitute(contribution: Contribution): String {
        return replace(SUBSTITUTION_AUTHOR_NAME, contribution.author)
    }
}
