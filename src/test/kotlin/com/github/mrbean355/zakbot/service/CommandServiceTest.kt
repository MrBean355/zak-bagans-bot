package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.BotUsername
import com.github.mrbean355.zakbot.telegram.TelegramAction
import com.github.mrbean355.zakbot.telegram.TelegramNotifier
import com.github.mrbean355.zakbot.reddit.RedditService
import com.github.mrbean355.zakbot.reddit.model.Comment
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.support.ResourceBundleMessageSource
import java.time.Instant

class CommandServiceTest {
    @MockK
    private lateinit var redditService: RedditService

    @MockK
    private lateinit var botStateService: BotStateService

    @MockK
    private lateinit var telegramNotifier: TelegramNotifier

    private lateinit var service: CommandService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { redditService.replyToComment(any(), any()) } returns "t1_reply"
        val messageSource = ResourceBundleMessageSource().apply {
            setBasename("strings")
            setDefaultEncoding("UTF-8")
            setUseCodeAsDefaultMessage(true)
            setAlwaysUseMessageFormat(true)
        }
        service = CommandService(redditService, botStateService, telegramNotifier, messageSource)
    }

    @Test
    fun testProcessAuthorCommand_IgnoreUser_AlreadyIgnored_DoesNothing() {
        val comment = createComment("u/$BotUsername ignore_user victim")
        every { botStateService.isUserIgnored("victim") } returns true

        service.processAuthorCommand(comment)

        verify {
            redditService.replyToComment(comment, "Hmm, 'victim' is already ignored 🤔")
        }
        verify(inverse = true) {
            botStateService.ignoreUser(any(), any())
        }
    }

    @Test
    fun testProcessAuthorCommand_IgnoreUser_NotAlreadyIgnored_UserDoesNotExist_DoesNothing() {
        val comment = createComment("u/$BotUsername ignore_user victim")
        every { botStateService.isUserIgnored("victim") } returns false
        every { redditService.userExists("victim") } returns false

        service.processAuthorCommand(comment)

        verify {
            redditService.replyToComment(comment, "Hmm, the user 'victim' doesn't seem to exist 🤔")
        }
        verify(inverse = true) {
            botStateService.ignoreUser(any(), any())
        }
    }

    @Test
    fun testProcessAuthorCommand_IgnoreUser_NotAlreadyIgnored_UserExists_IgnoresUser() {
        val comment = createComment("u/$BotUsername ignore_user victim")
        every { botStateService.isUserIgnored("victim") } returns false
        every { redditService.userExists("victim") } returns true

        service.processAuthorCommand(comment)

        verify {
            botStateService.ignoreUser("victim", "abc123")
            redditService.replyToComment(comment, "Okay, I will ignore 'victim' from now on 👌")
            telegramNotifier.sendMessage(
                match { it.contains("ignore_user") && it.contains("victim") },
                listOf(TelegramAction.UnignoreUser("victim"), TelegramAction.OpenUrl("www.reddit.com")),
            )
        }
    }

    @Test
    fun testProcessAuthorCommand_UnignoreUser_NotAlreadyIgnored_DoesNothing() {
        val comment = createComment("u/$BotUsername unignore_user victim")
        every { botStateService.isUserIgnored("victim") } returns false

        service.processAuthorCommand(comment)

        verify {
            redditService.replyToComment(comment, "Hmm, 'victim' is not ignored 🤔")
        }
        verify(inverse = true) {
            botStateService.unignoreUser(any())
            telegramNotifier.sendMessage(any(), any())
        }
    }

    @Test
    fun testProcessAuthorCommand_UnignoreUser_IsIgnored_UnignoresUser() {
        val comment = createComment("u/$BotUsername unignore_user victim")
        every { botStateService.isUserIgnored("victim") } returns true

        service.processAuthorCommand(comment)

        verify {
            botStateService.unignoreUser("victim")
            redditService.replyToComment(comment, "Okay, I won't ignore 'victim' anymore 👌")
            telegramNotifier.sendMessage(
                match { it.contains("unignore_user") && it.contains("victim") },
                listOf(TelegramAction.IgnoreUser("victim"), TelegramAction.OpenUrl("www.reddit.com")),
            )
        }
    }

    @Test
    fun testProcessAuthorCommand_IgnorePost_WithReason_NotAlreadyIgnored_IgnoresSubmission() {
        val comment = createComment("u/$BotUsername ignore_post This is a complex reason.")
        every { botStateService.isSubmissionIgnored("def456") } returns false

        service.processAuthorCommand(comment)

        verify {
            botStateService.ignoreSubmission("def456", "This is a complex reason.")
            redditService.replyToComment(comment, "Okay, I will ignore all comments on this post 👌")
            telegramNotifier.sendMessage(
                match { it.contains("ignore_post") && it.contains("This is a complex reason.") },
                listOf(TelegramAction.UnignoreSubmission("def456"), TelegramAction.OpenUrl("www.reddit.com")),
            )
        }
    }

    @Test
    fun testProcessAuthorCommand_IgnorePost_WithReason_AlreadyIgnored_DoesNothing() {
        val comment = createComment("u/$BotUsername ignore_post This is a complex reason.")
        every { botStateService.isSubmissionIgnored("def456") } returns true

        service.processAuthorCommand(comment)

        verify {
            redditService.replyToComment(comment, "Hmm, this post is already ignored 🤔")
        }
        verify(inverse = true) {
            botStateService.ignoreSubmission(any(), any())
            telegramNotifier.sendMessage(any(), any())
        }
    }

    @Test
    fun testProcessAuthorCommand_IgnorePost_WithoutReason_NotAlreadyIgnored_IgnoresSubmission() {
        val comment = createComment("u/$BotUsername ignore_post")
        every { botStateService.isSubmissionIgnored("def456") } returns false

        service.processAuthorCommand(comment)

        verify {
            botStateService.ignoreSubmission("def456", null)
            redditService.replyToComment(comment, "Okay, I will ignore all comments on this post 👌")
            telegramNotifier.sendMessage(
                match { it.contains("ignore_post") && it.contains("None") },
                listOf(TelegramAction.UnignoreSubmission("def456"), TelegramAction.OpenUrl("www.reddit.com")),
            )
        }
    }

    @Test
    fun testProcessAuthorCommand_IgnorePost_WithoutReason_AlreadyIgnored_DoesNothing() {
        val comment = createComment("u/$BotUsername ignore_post")
        every { botStateService.isSubmissionIgnored("def456") } returns true

        service.processAuthorCommand(comment)

        verify {
            redditService.replyToComment(comment, "Hmm, this post is already ignored 🤔")
        }
        verify(inverse = true) {
            botStateService.ignoreSubmission(any(), any())
            telegramNotifier.sendMessage(any(), any())
        }
    }

    @Test
    fun testProcessAuthorCommand_UnignorePost_IsIgnored_UnignoresSubmission() {
        val comment = createComment("u/$BotUsername unignore_post")
        every { botStateService.isSubmissionIgnored("def456") } returns true

        service.processAuthorCommand(comment)

        verify {
            botStateService.unignoreSubmission("def456")
            redditService.replyToComment(comment, "Okay, I won't ignore this post anymore 👌")
            telegramNotifier.sendMessage(
                match { it.contains("unignore_post") },
                listOf(TelegramAction.IgnoreSubmission("def456"), TelegramAction.OpenUrl("www.reddit.com")),
            )
        }
    }

    @Test
    fun testProcessAuthorCommand_UnignorePost_NotAlreadyIgnored_UnignoresSubmission() {
        val comment = createComment("u/$BotUsername unignore_post")
        every { botStateService.isSubmissionIgnored("def456") } returns false

        service.processAuthorCommand(comment)

        verify {
            redditService.replyToComment(comment, "Hmm, this post is not ignored 🤔")
        }
        verify(inverse = true) {
            botStateService.unignoreSubmission(any())
            telegramNotifier.sendMessage(any(), any())
        }
    }

    @Test
    fun testProcessAuthorCommand_LeadingSlash_ProcessesCommand() {
        val comment = createComment("/u/$BotUsername unignore_post")
        every { botStateService.isSubmissionIgnored("def456") } returns false

        service.processAuthorCommand(comment)

        verify {
            redditService.replyToComment(comment, "Hmm, this post is not ignored 🤔")
        }
    }

    @Test
    fun testProcessAuthorCommand_CaseInsensitive_ProcessesCommand() {
        val comment = createComment("U/$BotUsername unignore_post")
        every { botStateService.isSubmissionIgnored("def456") } returns false

        service.processAuthorCommand(comment)

        verify {
            redditService.replyToComment(comment, "Hmm, this post is not ignored 🤔")
        }
    }

    private fun createComment(body: String) = Comment(
        id = "abc123",
        fullName = "abc123",
        author = "tester",
        created = Instant.now(),
        url = "www.reddit.com",
        body = body,
        submissionFullName = "def456",
        parentFullName = "t1_parent",
    )
}

