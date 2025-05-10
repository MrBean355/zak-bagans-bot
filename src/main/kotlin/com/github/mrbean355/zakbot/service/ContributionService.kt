package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.AuthorUsername
import com.github.mrbean355.zakbot.BotUsername
import com.github.mrbean355.zakbot.TelegramNotifier
import com.github.mrbean355.zakbot.db.BotCache
import com.github.mrbean355.zakbot.util.getString
import net.dean.jraw.models.Comment
import net.dean.jraw.models.PublicContribution
import net.dean.jraw.models.Submission
import org.springframework.stereotype.Service

private const val SUBSTITUTION_AUTHOR_NAME = "{author}"

private val BOT_REGEX = """\bbot\b""".toRegex()

@Service
class ContributionService(
    private val redditService: RedditService,
    private val phraseService: PhraseService,
    private val commandService: CommandService,
    private val botCache: BotCache,
    private val telegramNotifier: TelegramNotifier,
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

        telegramNotifier.sendMessage(getString("telegram.new_submission", submission.title, response, submission.url))
        redditService.replyToSubmission(submission, response)
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
            if (comment.mentionsBadBot()) {
                botCache.ignoreUser(comment.author, comment.fullName)
                telegramNotifier.sendMessage(getString("telegram.new_ignored_user", comment.author, comment.url))
                redditService.replyToComment(comment, getString("reddit.new_user_ignored"))
            }
            return
        }

        val response = phraseService.findPhrase(comment)
            ?.substitute(comment)
            ?: return

        telegramNotifier.sendMessage(getString("telegram.new_comment", comment.body, response, comment.url))
        redditService.replyToComment(comment, response)
    }

    /**
     * Send a Telegram notification when someone mentions something 'bot' related.
     */
    private fun checkForBotMention(contribution: PublicContribution<*>) {
        val text = when (contribution) {
            is Submission -> contribution.title + contribution.selfText.orEmpty()
            is Comment -> contribution.body
            else -> return
        }.lowercase().trim()

        if (text == "good bot") {
            return
        }

        if (text.contains(BOT_REGEX) ||
            "zakbot" in text ||
            "zacbot" in text ||
            "baganbot" in text ||
            "bagansbot" in text
        ) {
            when (contribution) {
                is Submission -> telegramNotifier.sendMessage(getString("telegram.new_bot_mention_submission", contribution.title, contribution.url))
                is Comment -> telegramNotifier.sendMessage(getString("telegram.new_bot_mention_comment", contribution.body, contribution.url))
            }
        }
    }

    private fun PublicContribution<*>.isAuthorIgnored(): Boolean {
        return botCache.isUserIgnored(author)
    }

    private fun Comment.mentionsBadBot(): Boolean {
        return body.filter(Char::isLetter).equals("badbot", ignoreCase = true)
    }

    private fun String.substitute(contribution: PublicContribution<*>): String {
        return replace(SUBSTITUTION_AUTHOR_NAME, contribution.author)
    }
}