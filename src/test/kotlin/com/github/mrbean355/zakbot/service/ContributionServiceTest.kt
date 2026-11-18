package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.AuthorUsername
import com.github.mrbean355.zakbot.BotUsername
import com.github.mrbean355.zakbot.telegram.TelegramAction
import com.github.mrbean355.zakbot.telegram.TelegramNotifier
import com.github.mrbean355.zakbot.config.ZakbotProperties
import com.github.mrbean355.zakbot.db.PhraseType
import com.github.mrbean355.zakbot.reddit.RedditService
import com.github.mrbean355.zakbot.reddit.model.Comment
import com.github.mrbean355.zakbot.reddit.model.Submission
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant

class ContributionServiceTest {
    @MockK
    private lateinit var redditService: RedditService

    @MockK
    private lateinit var phraseService: PhraseService

    @MockK
    private lateinit var commandService: CommandService

    @MockK
    private lateinit var geminiService: GeminiService

    @MockK
    private lateinit var botStateService: BotStateService

    @MockK
    private lateinit var telegramNotifier: TelegramNotifier

    private lateinit var service: ContributionService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        justRun { telegramNotifier.sendMessage(any()) }
        justRun { telegramNotifier.sendMessage(any(), any()) }
        every { redditService.replyToSubmission(any(), any()) } returns "t1_reply"
        every { redditService.replyToComment(any(), any()) } returns "t1_reply"
        every { botStateService.isUserIgnored(any()) } returns false
        every { botStateService.isSubmissionIgnored(any()) } returns false
        every { geminiService.isDryRun } returns false
        every { geminiService.evaluateSubmission(any()) } returns GeminiService.ModerationResult(sensitivity = 0f, reason = null)
        every { geminiService.evaluateComment(any()) } returns GeminiService.ModerationResult(sensitivity = 0f, reason = null)
        every { geminiService.evaluateBotReply(any()) } returns GeminiService.BotSentiment.NEUTRAL
        every { phraseService.findPhrase(any<Submission>()) } returns null
        every { phraseService.findPhrase(any<Comment>()) } returns null
        every { phraseService.getPhrase(any()) } returns null
        every { redditService.getCommentSubmission(any()) } returns null
        every { redditService.findParentComment(any()) } returns null

        service = ContributionService(
            redditService = redditService,
            phraseService = phraseService,
            commandService = commandService,
            geminiService = geminiService,
            botStateService = botStateService,
            telegramNotifier = telegramNotifier,
            zakbotProperties = ZakbotProperties(),
            randomRoll = { 0f },
        )
    }

    @Test
    fun testProcessSubmission_WhenAuthorIsBotAuthor_DoesNothing() {
        val submission = createSubmission(author = AuthorUsername, title = "Title")

        service.processSubmission(submission)

        verify(exactly = 0) {
            redditService.replyToSubmission(any(), any())
            telegramNotifier.sendMessage(any())
        }
    }

    @Test
    fun testProcessSubmission_WhenAuthorIgnored_DoesNothing() {
        val submission = createSubmission(author = "bad_user", title = "Title")
        every { botStateService.isUserIgnored("bad_user") } returns true

        service.processSubmission(submission)

        verify(exactly = 0) {
            geminiService.evaluateSubmission(any())
            redditService.replyToSubmission(any(), any())
        }
    }

    @Test
    fun testProcessSubmission_WhenAlreadyIgnored_DoesNothing() {
        val submission = createSubmission(author = "user1", title = "Title")
        every { botStateService.isSubmissionIgnored(submission.fullName) } returns true

        service.processSubmission(submission)

        verify(exactly = 0) {
            geminiService.evaluateSubmission(any())
            redditService.replyToSubmission(any(), any())
        }
    }

    @Test
    fun testProcessSubmission_WhenSensitivityMeetsThreshold_IgnoresSubmissionAndSendsTelegramWithoutReplying() {
        val submission = createSubmission(author = "user1", title = "Sad news about a crew member")
        every { geminiService.evaluateSubmission(submission) } returns GeminiService.ModerationResult(
            sensitivity = 0.85f,
            reason = "Crew member obituary",
        )

        service.processSubmission(submission)

        verify {
            geminiService.evaluateSubmission(submission)
            botStateService.ignoreSubmission(submission.fullName, "Crew member obituary")
            telegramNotifier.sendMessage(
                match {
                    it.contains("Sensitive Post Ignored") &&
                        it.contains("Sad news about a crew member") &&
                        it.contains("85%") &&
                        it.contains("Crew member obituary")
                },
                any(),
            )
        }
        verify(exactly = 0) {
            phraseService.findPhrase(any<Submission>())
            redditService.replyToSubmission(any(), any())
        }
    }

    @Test
    fun testProcessSubmission_WhenSensitiveAndDryRunEnabled_SendsDryRunTelegramAndStillReplies() {
        val submission = createSubmission(author = "user1", title = "Sad news about a crew member")
        every { geminiService.isDryRun } returns true
        every { geminiService.evaluateSubmission(submission) } returns GeminiService.ModerationResult(
            sensitivity = 0.85f,
            reason = "Crew member obituary",
        )
        every { phraseService.findPhrase(submission) } returns "Some quote"

        service.processSubmission(submission)

        verify(exactly = 0) {
            botStateService.ignoreSubmission(any(), any())
        }
        verify {
            telegramNotifier.sendMessage(
                match {
                    it.contains("Sensitive Post Detected (Dry Run)") &&
                        it.contains("Sad news about a crew member") &&
                        it.contains("85%") &&
                        it.contains("Crew member obituary")
                },
                any(),
            )
            redditService.replyToSubmission(submission, "Some quote")
        }
    }

    @Test
    fun testProcessSubmission_WhenSensitivityBelowThreshold_Replies() {
        val submission = createSubmission(author = "user1", title = "Did you see Zak?")
        every { geminiService.evaluateSubmission(submission) } returns GeminiService.ModerationResult(
            sensitivity = 0.4f,
            reason = null,
        )
        every { phraseService.findPhrase(submission) } returns "We want answers"

        service.processSubmission(submission)

        verify(exactly = 0) {
            botStateService.ignoreSubmission(any(), any())
        }
        verify {
            redditService.replyToSubmission(submission, "We want answers")
        }
    }

    @Test
    fun testProcessSubmission_WhenSensitiveWithBlankReason_IgnoresWithNullReasonAndSendsNoneInTelegram() {
        val submission = createSubmission(author = "user1", title = "Serious post")
        every { geminiService.evaluateSubmission(submission) } returns GeminiService.ModerationResult(
            sensitivity = 0.9f,
            reason = "   ",
        )

        service.processSubmission(submission)

        verify {
            botStateService.ignoreSubmission(submission.fullName, null)
            telegramNotifier.sendMessage(
                match {
                    it.contains("Sensitive Post Ignored") &&
                        it.contains("Serious post") &&
                        it.contains("90%") &&
                        it.contains("None")
                },
                any(),
            )
        }
        verify(exactly = 0) {
            redditService.replyToSubmission(any(), any())
        }
    }

    @Test
    fun testProcessSubmission_WhenPhraseFound_SendsTelegramAndReplies() {
        val submission = createSubmission(author = "ghost_hunter", title = "Did you see Zak?")
        every { phraseService.findPhrase(submission) } returns "Look at this {author}"

        service.processSubmission(submission)

        verify {
            telegramNotifier.sendMessage(
                match { it.contains("ghost_hunter") || it.contains("Did you see Zak?") },
                listOf(
                    TelegramAction.IgnoreUser("ghost_hunter"),
                    TelegramAction.IgnoreSubmission(submission.fullName),
                    TelegramAction.DeleteComment("t1_reply"),
                    TelegramAction.OpenUrl(submission.url),
                ),
            )
            redditService.replyToSubmission(submission, "Look at this ghost_hunter")
        }
    }

    @Test
    fun testProcessComment_WhenAuthorIsBotAuthor_ProcessesAuthorCommand() {
        val comment = createComment(author = AuthorUsername, body = "u/$BotUsername ignore_user foo")

        service.processComment(comment)

        verify { commandService.processAuthorCommand(comment) }
        verify(exactly = 0) { redditService.replyToComment(any(), any()) }
    }

    @Test
    fun testProcessComment_WhenAuthorIsBotItself_DoesNothing() {
        val comment = createComment(author = BotUsername, body = "Bot reply")

        service.processComment(comment)

        verify(exactly = 0) { redditService.replyToComment(any(), any()) }
    }

    @Test
    fun testProcessComment_WhenBadBotReplyToBotComment_IgnoresUserAndRepliesWithoutCallingGemini() {
        val botComment = createComment(author = BotUsername, body = "I am Zak")
        val comment = createComment(author = "critic", body = "bad bot", parentFullName = "t1_bot")
        every { redditService.findParentComment(comment) } returns botComment

        service.processComment(comment)

        verify {
            botStateService.ignoreUser("critic", comment.fullName)
            telegramNotifier.sendMessage(
                match { it.contains("critic") },
                listOf(TelegramAction.UnignoreUser("critic"), TelegramAction.OpenUrl(comment.url)),
            )
            redditService.replyToComment(comment, match { it.contains("won't reply") })
        }
        verify(exactly = 0) {
            geminiService.evaluateBotReply(any())
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["good bot!", "great bot!", "best bot!", "goodbot", "Great bot", "BEST BOT!"])
    fun testProcessComment_WhenGoodBotReplyToBotComment_RepliesWithAppreciationWithoutCallingGemini(praise: String) {
        val botComment = createComment(author = BotUsername, body = "I am Zak")
        val comment = createComment(author = "fan", body = praise, parentFullName = "t1_bot")
        every { redditService.findParentComment(comment) } returns botComment
        every { phraseService.getPhrase(PhraseType.GoodBot) } returns "Thank you, {author}!"

        service.processComment(comment)

        verify {
            telegramNotifier.sendMessage(
                match { it.contains("fan") && it.contains("Thank you, fan!\n\n^(Thank you ❤️)") },
                listOf(
                    TelegramAction.IgnoreUser("fan"),
                    TelegramAction.DeleteComment("t1_reply"),
                    TelegramAction.OpenUrl(comment.url),
                ),
            )
            redditService.replyToComment(comment, "Thank you, fan!\n\n^(Thank you ❤️)")
        }
        verify(exactly = 0) {
            botStateService.ignoreUser(any(), any())
            geminiService.evaluateBotReply(any())
        }
    }

    @Test
    fun testProcessComment_WhenGeminiClassifiesReplyAsBadBot_IgnoresUserAndReplies() {
        val botComment = createComment(author = BotUsername, body = "I am Zak")
        val comment = createComment(author = "critic", body = "shut up stupid bot", parentFullName = "t1_bot")
        every { redditService.findParentComment(comment) } returns botComment
        every { geminiService.evaluateBotReply(comment) } returns GeminiService.BotSentiment.BAD_BOT

        service.processComment(comment)

        verify {
            geminiService.evaluateBotReply(comment)
            botStateService.ignoreUser("critic", comment.fullName)
            telegramNotifier.sendMessage(
                match { it.contains("User Ignored") && it.contains("critic") },
                listOf(TelegramAction.UnignoreUser("critic"), TelegramAction.OpenUrl(comment.url)),
            )
            redditService.replyToComment(comment, match { it.contains("won't reply") })
        }
    }

    @Test
    fun testProcessComment_WhenGeminiClassifiesReplyAsGoodBot_RepliesWithAppreciation() {
        val botComment = createComment(author = BotUsername, body = "I am Zak")
        val comment = createComment(author = "fan", body = "I love this bot so much!", parentFullName = "t1_bot")
        every { redditService.findParentComment(comment) } returns botComment
        every { geminiService.evaluateBotReply(comment) } returns GeminiService.BotSentiment.GOOD_BOT
        every { phraseService.getPhrase(PhraseType.GoodBot) } returns "Thank you, {author}!"

        service.processComment(comment)

        verify(exactly = 1) {
            geminiService.evaluateBotReply(comment)
            telegramNotifier.sendMessage(
                match { it.contains("Good Bot Praise") && it.contains("fan") },
                any(),
            )
            redditService.replyToComment(comment, "Thank you, fan!\n\n^(Thank you ❤️)")
        }
        verify(exactly = 0) {
            botStateService.ignoreUser(any(), any())
        }
    }

    @Test
    fun testProcessComment_WhenReplyToBotDoesNotMentionBot_DoesNotCallGemini() {
        val botComment = createComment(author = BotUsername, body = "I am Zak")
        val comment = createComment(author = "viewer", body = "Haha classic Zak", parentFullName = "t1_bot")
        every { redditService.findParentComment(comment) } returns botComment

        service.processComment(comment)

        verify(exactly = 0) {
            geminiService.evaluateBotReply(any())
            botStateService.ignoreUser(any(), any())
            redditService.replyToComment(any(), any())
            telegramNotifier.sendMessage(any())
        }
    }

    @Test
    fun testProcessComment_WhenGeminiClassifiesReplyAsNeutral_DoesNotIgnoreOrReply() {
        val botComment = createComment(author = BotUsername, body = "I am Zak")
        val comment = createComment(author = "viewer", body = "Is this a bot?", parentFullName = "t1_bot")
        every { redditService.findParentComment(comment) } returns botComment
        every { geminiService.evaluateBotReply(comment) } returns GeminiService.BotSentiment.NEUTRAL

        service.processComment(comment)

        verify(exactly = 1) {
            geminiService.evaluateBotReply(comment)
            telegramNotifier.sendMessage(match { it.contains("Bot Mentioned in Comment") }, any())
        }
        verify(exactly = 0) {
            botStateService.ignoreUser(any(), any())
            redditService.replyToComment(any(), any())
        }
    }

    @Test
    fun testProcessComment_WhenGeminiClassifiesReplyAsBadBotInDryRun_SendsDryRunTelegramWithoutIgnoringOrReplying() {
        val botComment = createComment(author = BotUsername, body = "I am Zak")
        val comment = createComment(author = "critic", body = "shut up stupid bot", parentFullName = "t1_bot")
        every { redditService.findParentComment(comment) } returns botComment
        every { geminiService.isDryRun } returns true
        every { geminiService.evaluateBotReply(comment) } returns GeminiService.BotSentiment.BAD_BOT

        service.processComment(comment)

        verify(exactly = 1) {
            geminiService.evaluateBotReply(comment)
            telegramNotifier.sendMessage(
                match {
                    it.contains("Bot Reply Classified (Dry Run)") &&
                        it.contains("critic") &&
                        it.contains("shut up stupid bot") &&
                        it.contains("BAD_BOT")
                },
                any(),
            )
        }
        verify(exactly = 0) {
            botStateService.ignoreUser(any(), any())
            redditService.replyToComment(any(), any())
        }
    }

    @Test
    fun testProcessComment_WhenGeminiClassifiesReplyAsGoodBotInDryRun_SendsDryRunTelegramWithoutReplying() {
        val botComment = createComment(author = BotUsername, body = "I am Zak")
        val comment = createComment(author = "fan", body = "I love this bot so much!", parentFullName = "t1_bot")
        every { redditService.findParentComment(comment) } returns botComment
        every { geminiService.isDryRun } returns true
        every { geminiService.evaluateBotReply(comment) } returns GeminiService.BotSentiment.GOOD_BOT
        every { phraseService.getPhrase(PhraseType.GoodBot) } returns "Thank you, {author}!"

        service.processComment(comment)

        verify(exactly = 1) {
            geminiService.evaluateBotReply(comment)
            telegramNotifier.sendMessage(
                match {
                    it.contains("Bot Reply Classified (Dry Run)") &&
                        it.contains("fan") &&
                        it.contains("GOOD_BOT")
                },
                any(),
            )
        }
        verify(exactly = 0) {
            botStateService.ignoreUser(any(), any())
            redditService.replyToComment(any(), any())
            phraseService.getPhrase(any())
        }
    }

    @Test
    fun testProcessComment_WhenGoodBotReplyToBotComment_NoPhraseFound_DoesNotReply() {
        val botComment = createComment(author = BotUsername, body = "I am Zak")
        val comment = createComment(author = "fan", body = "good bot!", parentFullName = "t1_bot")
        every { redditService.findParentComment(comment) } returns botComment
        every { phraseService.getPhrase(PhraseType.GoodBot) } returns null

        service.processComment(comment)

        verify(exactly = 0) {
            redditService.replyToComment(any(), any())
            telegramNotifier.sendMessage(any())
        }
    }

    @Test
    fun testProcessComment_WhenGoodBotReplyToBotComment_ChanceRollFails_DoesNotReply() {
        val testService = ContributionService(
            redditService = redditService,
            phraseService = phraseService,
            commandService = commandService,
            geminiService = geminiService,
            botStateService = botStateService,
            telegramNotifier = telegramNotifier,
            zakbotProperties = ZakbotProperties(),
            randomRoll = { 0.5f },
        )
        val botComment = createComment(author = BotUsername, body = "I am Zak")
        val comment = createComment(author = "fan", body = "good bot!", parentFullName = "t1_bot")
        every { redditService.findParentComment(comment) } returns botComment
        every { phraseService.getPhrase(PhraseType.GoodBot) } returns "Thank you, {author}!"

        testService.processComment(comment)

        verify(exactly = 0) {
            redditService.replyToComment(any(), any())
            telegramNotifier.sendMessage(any())
            phraseService.getPhrase(any())
        }
    }

    @Test
    fun testProcessComment_WhenPhraseFound_Replies() {
        val comment = createComment(author = "critic", body = "Aaron is crazy")
        every { redditService.findParentComment(comment) } returns null
        every { redditService.getCommentSubmission(comment) } returns null
        every { phraseService.findPhrase(comment) } returns "Aaron, get in there"

        service.processComment(comment)

        verify {
            geminiService.evaluateComment(comment)
            redditService.replyToComment(comment, "Aaron, get in there")
        }
    }

    @Test
    fun testProcessComment_WhenNoPhraseFound_DoesNotCallEvaluateComment() {
        val comment = createComment(author = "critic", body = "Nothing matching here")
        every { phraseService.findPhrase(comment) } returns null

        service.processComment(comment)

        verify(exactly = 0) {
            geminiService.evaluateComment(any())
            redditService.replyToComment(any(), any())
        }
    }

    @Test
    fun testProcessComment_WhenPhraseFoundAndCommentSensitive_SkipsReplyAndSendsTelegram() {
        val comment = createComment(author = "user1", body = "My dad passed away, I feel so sad")
        every { phraseService.findPhrase(comment) } returns "Some quote"
        every { geminiService.evaluateComment(comment) } returns GeminiService.ModerationResult(
            sensitivity = 0.9f,
            reason = "Personal bereavement",
        )

        service.processComment(comment)

        verify(exactly = 1) {
            geminiService.evaluateComment(comment)
            telegramNotifier.sendMessage(
                match {
                    it.contains("Sensitive Comment Skipped") &&
                        it.contains("My dad passed away, I feel so sad") &&
                        it.contains("90%") &&
                        it.contains("Personal bereavement")
                },
                any(),
            )
        }
        verify(exactly = 0) {
            redditService.replyToComment(any(), any())
        }
    }

    @Test
    fun testProcessComment_WhenPhraseFoundAndCommentSensitiveInDryRun_SendsDryRunTelegramAndStillReplies() {
        val comment = createComment(author = "user1", body = "My dad passed away, I feel so sad")
        every { phraseService.findPhrase(comment) } returns "Some quote"
        every { geminiService.isDryRun } returns true
        every { geminiService.evaluateComment(comment) } returns GeminiService.ModerationResult(
            sensitivity = 0.9f,
            reason = "Personal bereavement",
        )

        service.processComment(comment)

        verify {
            geminiService.evaluateComment(comment)
            telegramNotifier.sendMessage(
                match {
                    it.contains("Sensitive Comment Detected (Dry Run)") &&
                        it.contains("My dad passed away, I feel so sad") &&
                        it.contains("90%") &&
                        it.contains("Personal bereavement")
                },
                any(),
            )
            redditService.replyToComment(comment, "Some quote")
        }
    }

    @Test
    fun testProcessSubmission_WhenMentionsBot_SendsTelegramNotification() {
        val submission = createSubmission(author = "ghost_hunter", title = "Is this zakbot real?")

        service.processSubmission(submission)

        verify {
            telegramNotifier.sendMessage(match { it.contains("Is this zakbot real?") }, any())
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["Good bot!", "Great bot!", "Best bot!"])
    fun testProcessSubmission_WhenMentionsGoodBot_DoesNotSendNotification(praise: String) {
        val submission = createSubmission(author = "ghost_hunter", title = praise)

        service.processSubmission(submission)

        verify(exactly = 0) {
            telegramNotifier.sendMessage(match { it.contains(praise) }, any())
        }
    }

    @Test
    fun testProcessComment_WhenMentionsBot_SendsTelegramNotification() {
        val comment = createComment(author = "commenter", body = "What a bot")

        service.processComment(comment)

        verify {
            telegramNotifier.sendMessage(match { it.contains("What a bot") }, any())
        }
    }

    @Test
    fun testProcessComment_WhenSubmissionIsIgnored_DoesNothing() {
        val comment = createComment(author = "commenter", body = "Hello there")
        every { botStateService.isSubmissionIgnored(comment.submissionFullName) } returns true

        service.processComment(comment)

        verify(exactly = 0) {
            redditService.replyToComment(any(), any())
        }
    }

    @Test
    fun testProcessComment_WhenParentSubmissionAuthorIsIgnored_DoesNothing() {
        val comment = createComment(author = "commenter", body = "Hello there")
        val submission = createSubmission(author = "ignored_author")
        every { redditService.getCommentSubmission(comment) } returns submission
        every { botStateService.isUserIgnored("ignored_author") } returns true

        service.processComment(comment)

        verify(exactly = 0) {
            redditService.replyToComment(any(), any())
        }
    }

    @Test
    fun testCheckDownvotedComments_WhenCommentAtOrBelowThreshold_DeletesAndNotifies() {
        val downvoted = createComment(id = "down1", author = BotUsername, body = "Terrible quote", score = -3)
        every { redditService.getOwnRecentComments() } returns listOf(downvoted)
        every { redditService.deleteComment(any()) } returns true

        service.checkDownvotedComments()

        verify(exactly = 1) {
            redditService.deleteComment(downvoted)
            telegramNotifier.sendMessage(
                match { it.contains("Downvoted Comment Deleted") && it.contains("-3") },
                listOf(TelegramAction.OpenUrl(downvoted.url)),
            )
        }
    }

    @Test
    fun testCheckDownvotedComments_WhenDeleteFails_DoesNotNotifyAndRetriesOnNextCheck() {
        val downvoted = createComment(id = "down1", author = BotUsername, body = "Terrible quote", score = -3)
        every { redditService.getOwnRecentComments() } returns listOf(downvoted)
        every { redditService.deleteComment(downvoted) } returnsMany listOf(false, true)

        service.checkDownvotedComments()

        verify(exactly = 1) { redditService.deleteComment(downvoted) }
        verify(exactly = 0) { telegramNotifier.sendMessage(any(), any()) }

        service.checkDownvotedComments()

        verify(exactly = 2) { redditService.deleteComment(downvoted) }
        verify(exactly = 1) { telegramNotifier.sendMessage(match { it.contains("Downvoted Comment Deleted") }, any()) }
    }

    @Test
    fun testCheckDownvotedComments_WhenCommentAboveThreshold_DoesNotDelete() {
        val popular = createComment(id = "pop1", author = BotUsername, body = "Great quote", score = 3)
        val mildlyDownvoted = createComment(id = "neu1", author = BotUsername, body = "Okay quote", score = -2)
        every { redditService.getOwnRecentComments() } returns listOf(popular, mildlyDownvoted)

        service.checkDownvotedComments()

        verify(exactly = 0) {
            redditService.deleteComment(any())
            telegramNotifier.sendMessage(any())
        }
    }

    @Test
    fun testCheckDownvotedComments_WhenCommentAlreadyDeleted_DoesNotDeleteOrNotify() {
        val deleted = createComment(id = "del1", author = BotUsername, body = "[deleted]", score = -5)
        every { redditService.getOwnRecentComments() } returns listOf(deleted)

        service.checkDownvotedComments()

        verify(exactly = 0) {
            redditService.deleteComment(any())
            telegramNotifier.sendMessage(any())
        }
    }

    @Test
    fun testCheckDownvotedComments_WhenCommentAlreadyProcessed_DoesNotDeleteTwice() {
        val downvoted = createComment(id = "down1", author = BotUsername, body = "Terrible quote", score = -3)
        every { redditService.getOwnRecentComments() } returns listOf(downvoted)
        every { redditService.deleteComment(any()) } returns true

        service.checkDownvotedComments()
        service.checkDownvotedComments()

        verify(exactly = 1) {
            redditService.deleteComment(downvoted)
            telegramNotifier.sendMessage(any(), any())
        }
    }

    @Test
    fun testCheckDownvotedComments_WhenDownvotesDisabled_DoesNothing() {
        val customService = ContributionService(
            redditService = redditService,
            phraseService = phraseService,
            commandService = commandService,
            geminiService = geminiService,
            botStateService = botStateService,
            telegramNotifier = telegramNotifier,
            zakbotProperties = ZakbotProperties(downvotes = ZakbotProperties.Downvotes(enabled = false)),
        )
        val downvoted = createComment(id = "down1", author = BotUsername, body = "Terrible quote", score = -3)
        every { redditService.getOwnRecentComments() } returns listOf(downvoted)

        customService.checkDownvotedComments()

        verify(exactly = 0) {
            redditService.deleteComment(any())
            telegramNotifier.sendMessage(any())
        }
    }

    private fun createSubmission(
        id: String = "sub1",
        author: String = "user1",
        title: String = "Title",
        selfText: String? = null,
    ) = Submission(
        id = id,
        fullName = "t3_$id",
        author = author,
        created = Instant.now(),
        url = "https://reddit.com/r/GhostAdventures/comments/$id",
        title = title,
        selfText = selfText,
    )

    private fun createComment(
        id: String = "com1",
        author: String = "user1",
        body: String = "Body",
        parentFullName: String = "t3_sub1",
        score: Int = 0,
    ) = Comment(
        id = id,
        fullName = "t1_$id",
        author = author,
        created = Instant.now(),
        url = "https://reddit.com/r/GhostAdventures/comments/sub1/title/$id",
        body = body,
        submissionFullName = "t3_sub1",
        parentFullName = parentFullName,
        score = score,
    )
}

