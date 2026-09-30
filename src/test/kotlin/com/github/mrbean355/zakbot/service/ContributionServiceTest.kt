package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.AuthorUsername
import com.github.mrbean355.zakbot.BotUsername
import com.github.mrbean355.zakbot.TelegramNotifier
import com.github.mrbean355.zakbot.db.BotCache
import com.github.mrbean355.zakbot.reddit.model.Comment
import com.github.mrbean355.zakbot.reddit.model.Submission
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class ContributionServiceTest {
    @MockK
    private lateinit var redditService: RedditService

    @MockK
    private lateinit var phraseService: PhraseService

    @MockK
    private lateinit var commandService: CommandService

    @MockK
    private lateinit var botCache: BotCache

    @MockK
    private lateinit var telegramNotifier: TelegramNotifier

    private lateinit var service: ContributionService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        justRun { telegramNotifier.sendMessage(any()) }
        justRun { redditService.replyToSubmission(any(), any()) }
        justRun { redditService.replyToComment(any(), any()) }
        every { botCache.isUserIgnored(any()) } returns false
        every { botCache.isSubmissionIgnored(any()) } returns false
        every { phraseService.findPhrase(any<Submission>()) } returns null
        every { phraseService.findPhrase(any<Comment>()) } returns null
        every { redditService.getCommentSubmission(any()) } returns null
        every { redditService.findParentComment(any()) } returns null

        service = ContributionService(
            redditService = redditService,
            phraseService = phraseService,
            commandService = commandService,
            botCache = botCache,
            telegramNotifier = telegramNotifier,
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
        every { botCache.isUserIgnored("bad_user") } returns true

        service.processSubmission(submission)

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
            telegramNotifier.sendMessage(match { it.contains("ghost_hunter") || it.contains("Did you see Zak?") })
            redditService.replyToSubmission(submission, "Look at this ghost_hunter")
        }
    }

    @Test
    fun testProcessComment_WhenAuthorIsBotAuthor_ProcessesAuthorCommand() {
        val comment = createComment(author = AuthorUsername, body = "!$BotUsername ignore_user foo")

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
    fun testProcessComment_WhenBadBotReplyToBotComment_IgnoresUserAndReplies() {
        val botComment = createComment(author = BotUsername, body = "I am Zak")
        val comment = createComment(author = "critic", body = "bad bot", parentFullName = "t1_bot")
        every { redditService.findParentComment(comment) } returns botComment

        service.processComment(comment)

        verify {
            botCache.ignoreUser("critic", comment.fullName)
            telegramNotifier.sendMessage(match { it.contains("critic") })
            redditService.replyToComment(comment, match { it.contains("won't reply") })
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
            redditService.replyToComment(comment, "Aaron, get in there")
        }
    }

    @Test
    fun testProcessSubmission_WhenMentionsBot_SendsTelegramNotification() {
        val submission = createSubmission(author = "ghost_hunter", title = "Is this zakbot real?")

        service.processSubmission(submission)

        verify {
            telegramNotifier.sendMessage(match { it.contains("Is this zakbot real?") })
        }
    }

    @Test
    fun testProcessSubmission_WhenMentionsGoodBot_DoesNotSendNotification() {
        val submission = createSubmission(author = "ghost_hunter", title = "Good bot!")

        service.processSubmission(submission)

        verify(exactly = 0) {
            telegramNotifier.sendMessage(match { it.contains("Good bot!") })
        }
    }

    @Test
    fun testProcessComment_WhenMentionsBot_SendsTelegramNotification() {
        val comment = createComment(author = "commenter", body = "What a bot")

        service.processComment(comment)

        verify {
            telegramNotifier.sendMessage(match { it.contains("What a bot") })
        }
    }

    @Test
    fun testProcessComment_WhenSubmissionIsIgnored_DoesNothing() {
        val comment = createComment(author = "commenter", body = "Hello there")
        every { botCache.isSubmissionIgnored(comment.submissionFullName) } returns true

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
        every { botCache.isUserIgnored("ignored_author") } returns true

        service.processComment(comment)

        verify(exactly = 0) {
            redditService.replyToComment(any(), any())
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
    ) = Comment(
        id = id,
        fullName = "t1_$id",
        author = author,
        created = Instant.now(),
        url = "https://reddit.com/r/GhostAdventures/comments/sub1/title/$id",
        body = body,
        submissionFullName = "t3_sub1",
        parentFullName = parentFullName,
    )
}
