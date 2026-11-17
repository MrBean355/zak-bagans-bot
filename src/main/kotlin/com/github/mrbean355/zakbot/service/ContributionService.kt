package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.AuthorUsername
import com.github.mrbean355.zakbot.BotUsername
import com.github.mrbean355.zakbot.telegram.TelegramAction
import com.github.mrbean355.zakbot.telegram.TelegramNotifier
import com.github.mrbean355.zakbot.config.ZakbotProperties
import com.github.mrbean355.zakbot.db.BotCache
import com.github.mrbean355.zakbot.db.PhraseType
import com.github.mrbean355.zakbot.reddit.RedditService
import com.github.mrbean355.zakbot.reddit.model.Comment
import com.github.mrbean355.zakbot.reddit.model.Contribution
import com.github.mrbean355.zakbot.reddit.model.Submission
import com.github.mrbean355.zakbot.service.GeminiService.BotSentiment
import com.github.mrbean355.zakbot.util.getString
import org.springframework.stereotype.Service
import java.util.Collections
import kotlin.math.roundToInt
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
    private val geminiService: GeminiService,
    private val botCache: BotCache,
    private val telegramNotifier: TelegramNotifier,
    private val zakbotProperties: ZakbotProperties,
    private val randomRoll: () -> Float = { Random.nextFloat() },
) {

    private val deletedCommentIds = Collections.newSetFromMap(
        Collections.synchronizedMap(
            object : LinkedHashMap<String, Boolean>(64, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?) = size > 200
            }
        )
    )

    /**
     * Checks the bot's recent comments and deletes any that have fallen below
     * the configured downvote threshold.
     */
    fun checkDownvotedComments() {
        if (!zakbotProperties.downvotes.enabled) {
            return
        }

        val comments = redditService.getOwnRecentComments()
        for (comment in comments) {
            if (comment.body != "[deleted]" &&
                comment.fullName !in deletedCommentIds &&
                comment.score <= zakbotProperties.downvotes.deleteThreshold &&
                redditService.deleteComment(comment)
            ) {
                deletedCommentIds += comment.fullName
                telegramNotifier.sendMessage(
                    getString(
                        "telegram.deleted_downvoted_comment",
                        comment.score,
                        comment.body,
                    ),
                    listOf(TelegramAction.OpenUrl(comment.url)),
                )
            }
        }
    }

    /**
     * Ignores submissions that are:
     * - sent by the bot's author, or
     * - sent by an ignored user, or
     * - already ignored, or
     * - flagged as sensitive by Gemini.
     */
    fun processSubmission(submission: Submission) {
        if (submission.author == AuthorUsername) {
            return
        }

        checkForBotMention(submission)

        if (submission.isAuthorIgnored() || botCache.isSubmissionIgnored(submission.fullName)) {
            return
        }

        val moderation = geminiService.evaluateSubmission(submission)
        if (moderation.sensitivity >= zakbotProperties.moderation.sensitivityThreshold) {
            val reason = moderation.reason?.ifBlank { null }
            val sensitivityPercent = (moderation.sensitivity * 100).roundToInt()
            if (geminiService.isDryRun) {
                telegramNotifier.sendMessage(
                    getString(
                        "telegram.sensitive_submission_dry_run",
                        submission.title,
                        sensitivityPercent,
                        reason ?: "None",
                    ),
                    listOf(
                        TelegramAction.IgnoreUser(submission.author),
                        TelegramAction.IgnoreSubmission(submission.fullName),
                        TelegramAction.OpenUrl(submission.url),
                    ),
                )
            } else {
                botCache.ignoreSubmission(submission.fullName, reason)
                telegramNotifier.sendMessage(
                    getString(
                        "telegram.sensitive_submission_ignored",
                        submission.title,
                        sensitivityPercent,
                        reason ?: "None",
                    ),
                    listOf(
                        TelegramAction.IgnoreUser(submission.author),
                        TelegramAction.UnignoreSubmission(submission.fullName),
                        TelegramAction.OpenUrl(submission.url),
                    ),
                )
                return
            }
        }

        val response = phraseService.findPhrase(submission)
            ?.substitute(submission)
            ?: return

        val replyId = redditService.replyToSubmission(submission, response)
        telegramNotifier.sendMessage(
            getString("telegram.new_submission", submission.title, response),
            replyActions(submission.author, submission.fullName, submission.url, replyId),
        )
    }

    /**
     * Ignore comments that are:
     * - sent by the bot's author that aren't commands, or
     * - sent by the bot itself, or
     * - sent by an ignored user, or
     * - replies to an ignored submission (via ignore command), or
     * - replies to a submission created by an ignored user, or
     * - replies to a comment sent by the bot, or
     * - flagged as sensitive by Gemini.
     */
    fun processComment(comment: Comment) {
        if (comment.author == AuthorUsername) {
            commandService.processAuthorCommand(comment)
            return
        }

        val mentionsBot = checkForBotMention(comment)

        if (comment.author == BotUsername ||
            comment.isAuthorIgnored() ||
            botCache.isSubmissionIgnored(comment.submissionFullName) ||
            redditService.getCommentSubmission(comment)?.isAuthorIgnored() == true
        ) {
            return
        }

        if (redditService.findParentComment(comment)?.author == BotUsername) {
            when (evaluateBotReplySentiment(comment, mentionsBot)) {
                BotSentiment.BAD_BOT -> {
                    botCache.ignoreUser(comment.author, comment.fullName)
                    redditService.replyToComment(comment, getString("reddit.new_user_ignored"))
                    telegramNotifier.sendMessage(
                        getString("telegram.new_ignored_user", comment.author),
                        listOf(TelegramAction.UnignoreUser(comment.author), TelegramAction.OpenUrl(comment.url)),
                    )
                }

                BotSentiment.GOOD_BOT -> {
                    if (randomRoll() <= GOOD_BOT_REPLY_CHANCE) {
                        val phrase = phraseService.getPhrase(PhraseType.GoodBot)
                            ?.substitute(comment)
                        if (phrase != null) {
                            val response = getString("reddit.good_bot_footer", phrase)
                            val replyId = redditService.replyToComment(comment, response)
                            telegramNotifier.sendMessage(
                                getString("telegram.good_bot_praise", comment.author, response),
                                buildList {
                                    add(TelegramAction.IgnoreUser(comment.author))
                                    if (replyId != null) {
                                        add(TelegramAction.DeleteComment(replyId))
                                    }
                                    add(TelegramAction.OpenUrl(comment.url))
                                },
                            )
                        }
                    }
                }

                BotSentiment.NEUTRAL -> Unit
            }
            return
        }

        val response = phraseService.findPhrase(comment)
            ?.substitute(comment)
            ?: return

        val moderation = geminiService.evaluateComment(comment)
        if (moderation.sensitivity >= zakbotProperties.moderation.sensitivityThreshold) {
            val reason = moderation.reason?.ifBlank { null } ?: "None"
            val sensitivityPercent = (moderation.sensitivity * 100).roundToInt()
            if (geminiService.isDryRun) {
                telegramNotifier.sendMessage(
                    getString(
                        "telegram.sensitive_comment_dry_run",
                        comment.body,
                        sensitivityPercent,
                        reason,
                    ),
                    listOf(
                        TelegramAction.IgnoreUser(comment.author),
                        TelegramAction.IgnoreSubmission(comment.submissionFullName),
                        TelegramAction.OpenUrl(comment.url),
                    ),
                )
            } else {
                telegramNotifier.sendMessage(
                    getString(
                        "telegram.sensitive_comment_skipped",
                        comment.body,
                        sensitivityPercent,
                        reason,
                    ),
                    listOf(
                        TelegramAction.IgnoreUser(comment.author),
                        TelegramAction.IgnoreSubmission(comment.submissionFullName),
                        TelegramAction.OpenUrl(comment.url),
                    ),
                )
                return
            }
        }

        val replyId = redditService.replyToComment(comment, response)
        telegramNotifier.sendMessage(
            getString("telegram.new_comment", comment.body, response),
            replyActions(comment.author, comment.submissionFullName, comment.url, replyId),
        )
    }

    private fun evaluateBotReplySentiment(comment: Comment, mentionsBot: Boolean): BotSentiment {
        if (comment.body.isBadBot()) {
            return BotSentiment.BAD_BOT
        }
        if (comment.body.isGoodBot()) {
            return BotSentiment.GOOD_BOT
        }
        if (!mentionsBot) {
            return BotSentiment.NEUTRAL
        }
        val sentiment = geminiService.evaluateBotReply(comment)
        if (geminiService.isDryRun) {
            if (sentiment != BotSentiment.NEUTRAL) {
                telegramNotifier.sendMessage(
                    getString(
                        "telegram.bot_reply_sentiment_dry_run",
                        comment.author,
                        comment.body,
                        sentiment.name,
                    ),
                    listOf(TelegramAction.IgnoreUser(comment.author), TelegramAction.OpenUrl(comment.url)),
                )
            }
            return BotSentiment.NEUTRAL
        }
        return sentiment
    }

    /**
     * Send a Telegram notification when someone mentions something 'bot' related.
     */
    private fun checkForBotMention(contribution: Contribution): Boolean {
        val text = when (contribution) {
            is Submission -> contribution.title + contribution.selfText.orEmpty()
            is Comment -> contribution.body
        }.lowercase().trim()

        if (text.isGoodBot()) {
            return false
        }

        if (text.contains(BOT_REGEX) ||
            "zakbot" in text ||
            "zacbot" in text ||
            "baganbot" in text ||
            "bagansbot" in text
        ) {
            when (contribution) {
                is Submission -> telegramNotifier.sendMessage(
                    getString("telegram.new_bot_mention_submission", contribution.title),
                    buildList {
                        if (contribution.author != BotUsername) {
                            add(TelegramAction.IgnoreUser(contribution.author))
                        }
                        add(TelegramAction.IgnoreSubmission(contribution.fullName))
                        add(TelegramAction.OpenUrl(contribution.url))
                    },
                )

                is Comment -> telegramNotifier.sendMessage(
                    getString("telegram.new_bot_mention_comment", contribution.body),
                    buildList {
                        if (contribution.author != BotUsername) {
                            add(TelegramAction.IgnoreUser(contribution.author))
                        }
                        add(TelegramAction.IgnoreSubmission(contribution.submissionFullName))
                        add(TelegramAction.OpenUrl(contribution.url))
                    },
                )
            }
            return true
        }
        return false
    }

    private fun replyActions(
        author: String,
        submissionFullName: String,
        url: String,
        replyFullName: String?,
    ): List<TelegramAction> = buildList {
        add(TelegramAction.IgnoreUser(author))
        add(TelegramAction.IgnoreSubmission(submissionFullName))
        if (replyFullName != null) {
            add(TelegramAction.DeleteComment(replyFullName))
        }
        add(TelegramAction.OpenUrl(url))
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
