package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.BotUsername
import com.github.mrbean355.zakbot.RedditService
import com.github.mrbean355.zakbot.db.BotCache
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import net.dean.jraw.models.Comment
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CommandServiceTest {
    @MockK
    private lateinit var redditService: RedditService

    @MockK
    private lateinit var botCache: BotCache

    @MockK
    private lateinit var comment: Comment
    private lateinit var service: CommandService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { comment.fullName } returns "abc123"
        every { comment.submissionFullName } returns "def456"
        every { comment.author } returns "tester"
        every { comment.url } returns "www.reddit.com"
        service = CommandService(redditService, botCache)
    }

    @Test
    fun testProcessAuthorCommand_IgnoreUser_AlreadyIgnored_DoesNothing() {
        every { comment.body } returns "!$BotUsername ignore\\_user victim"
        every { botCache.isUserIgnored("victim") } returns true

        service.processAuthorCommand(comment)

        verify {
            redditService.replyToComment(comment, "Hmm, 'victim' is already ignored 🤔")
        }
        verify(inverse = true) {
            botCache.ignoreUser(any(), any())
        }
    }

    @Test
    fun testProcessAuthorCommand_IgnoreUser_NotAlreadyIgnored_UserDoesNotExist_DoesNothing() {
        every { comment.body } returns "!$BotUsername ignore\\_user victim"
        every { botCache.isUserIgnored("victim") } returns false
        every { redditService.userExists("victim") } returns false

        service.processAuthorCommand(comment)

        verify {
            redditService.replyToComment(comment, "Hmm, the user 'victim' doesn't seem to exist 🤔")
        }
        verify(inverse = true) {
            botCache.ignoreUser(any(), any())
        }
    }

    @Test
    fun testProcessAuthorCommand_IgnoreUser_NotAlreadyIgnored_UserExists_IgnoresUser() {
        every { comment.body } returns "!$BotUsername ignore\\_user victim"
        every { botCache.isUserIgnored("victim") } returns false
        every { redditService.userExists("victim") } returns true

        service.processAuthorCommand(comment)

        verify {
            botCache.ignoreUser("victim", "abc123")
            redditService.replyToComment(comment, "Okay, I will ignore 'victim' from now on 👌")
        }
    }

    @Test
    fun testProcessAuthorCommand_UnignoreUser_NotAlreadyIgnored_DoesNothing() {
        every { comment.body } returns "!$BotUsername unignore\\_user victim"
        every { botCache.isUserIgnored("victim") } returns false

        service.processAuthorCommand(comment)

        verify {
            redditService.replyToComment(comment, "Hmm, 'victim' is not ignored 🤔")
        }
        verify(inverse = true) {
            botCache.unignoreUser(any())
        }
    }

    @Test
    fun testProcessAuthorCommand_UnignoreUser_IsIgnored_UnignoresUser() {
        every { comment.body } returns "!$BotUsername unignore\\_user victim"
        every { botCache.isUserIgnored("victim") } returns true

        service.processAuthorCommand(comment)

        verify {
            botCache.unignoreUser("victim")
            redditService.replyToComment(comment, "Okay, I won't ignore 'victim' anymore 👌")
        }
    }

    @Test
    fun testProcessAuthorCommand_IgnorePost_WithReason_NotAlreadyIgnored_IgnoresSubmission() {
        every { comment.body } returns "!$BotUsername ignore\\_post This is a complex reason."
        every { botCache.isSubmissionIgnored("def456") } returns false

        service.processAuthorCommand(comment)

        verify {
            botCache.ignoreSubmission("def456", "This is a complex reason.")
            redditService.replyToComment(comment, "Okay, I will ignore all comments on this post 👌")
        }
    }

    @Test
    fun testProcessAuthorCommand_IgnorePost_WithReason_AlreadyIgnored_DoesNothing() {
        every { comment.body } returns "!$BotUsername ignore\\_post This is a complex reason."
        every { botCache.isSubmissionIgnored("def456") } returns true

        service.processAuthorCommand(comment)

        verify {
            redditService.replyToComment(comment, "Hmm, this post is already ignored 🤔")
        }
        verify(inverse = true) {
            botCache.ignoreSubmission(any(), any())
        }
    }

    @Test
    fun testProcessAuthorCommand_IgnorePost_WithoutReason_NotAlreadyIgnored_IgnoresSubmission() {
        every { comment.body } returns "!$BotUsername ignore\\_post"
        every { botCache.isSubmissionIgnored("def456") } returns false

        service.processAuthorCommand(comment)

        verify {
            botCache.ignoreSubmission("def456", null)
            redditService.replyToComment(comment, "Okay, I will ignore all comments on this post 👌")
        }
    }

    @Test
    fun testProcessAuthorCommand_IgnorePost_WithoutReason_AlreadyIgnored_DoesNothing() {
        every { comment.body } returns "!$BotUsername ignore\\_post"
        every { botCache.isSubmissionIgnored("def456") } returns true

        service.processAuthorCommand(comment)

        verify {
            redditService.replyToComment(comment, "Hmm, this post is already ignored 🤔")
        }
        verify(inverse = true) {
            botCache.ignoreSubmission(any(), any())
        }
    }

    @Test
    fun testProcessAuthorCommand_UnignorePost_IsIgnored_UnignoresSubmission() {
        every { comment.body } returns "!$BotUsername unignore\\_post"
        every { botCache.isSubmissionIgnored("def456") } returns true

        service.processAuthorCommand(comment)

        verify {
            botCache.unignoreSubmission("def456")
            redditService.replyToComment(comment, "Okay, I won't ignore this post anymore 👌")
        }
    }

    @Test
    fun testProcessAuthorCommand_UnignorePost_NotAlreadyIgnored_UnignoresSubmission() {
        every { comment.body } returns "!$BotUsername unignore\\_post"
        every { botCache.isSubmissionIgnored("def456") } returns false

        service.processAuthorCommand(comment)

        verify {
            redditService.replyToComment(comment, "Hmm, this post is not ignored 🤔")
        }
        verify(inverse = true) {
            botCache.unignoreSubmission(any())
        }
    }
}
