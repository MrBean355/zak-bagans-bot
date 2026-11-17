package com.github.mrbean355.zakbot.telegram

import com.github.mrbean355.zakbot.config.GeminiProperties
import com.github.mrbean355.zakbot.config.TelegramProperties
import com.github.mrbean355.zakbot.db.BotCache
import com.github.mrbean355.zakbot.db.repo.IgnoredSubmissionRepository
import com.github.mrbean355.zakbot.db.repo.IgnoredUserRepository
import com.github.mrbean355.zakbot.db.repo.PhraseRepository
import com.github.mrbean355.zakbot.reddit.RedditService
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.boot.info.BuildProperties
import org.springframework.context.ApplicationContext
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.generics.TelegramClient
import java.util.Properties

class TelegramBotTest {
    @MockK
    private lateinit var applicationContext: ApplicationContext

    @MockK
    private lateinit var phraseRepository: PhraseRepository

    @MockK
    private lateinit var ignoredUserRepository: IgnoredUserRepository

    @MockK
    private lateinit var ignoredSubmissionRepository: IgnoredSubmissionRepository

    @MockK
    private lateinit var botCache: BotCache

    @MockK
    private lateinit var redditService: RedditService

    @MockK
    private lateinit var telegramClient: TelegramClient

    private lateinit var geminiProperties: GeminiProperties
    private lateinit var bot: TelegramBot

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { applicationContext.startupDate } returns System.currentTimeMillis() - 3600_000L
        every { phraseRepository.count() } returns 150L
        every { ignoredUserRepository.count() } returns 7L
        every { ignoredSubmissionRepository.count() } returns 3L
        every { telegramClient.execute(any<SendMessage>()) } returns mockk()
        every { telegramClient.execute(any<AnswerCallbackQuery>()) } returns true
        every { telegramClient.execute(any<EditMessageReplyMarkup>()) } returns mockk()
        every { telegramClient.execute(any<EditMessageText>()) } returns mockk()

        geminiProperties = GeminiProperties(apiKey = "test-key", enabled = true, dryRun = false)
        bot = createBot()
    }

    @Test
    fun testSendMessage_SendsToConfiguredChatId() {
        bot.sendMessage("Hello test")

        verify {
            telegramClient.execute(
                match<SendMessage> { message ->
                    message.chatId == "123456" &&
                        message.text == "Hello test" &&
                        message.replyMarkup == null
                }
            )
        }
    }

    @Test
    fun testSendMessage_WithActions_BuildsInlineKeyboard() {
        bot.sendMessage(
            "New post",
            listOf(
                TelegramAction.IgnoreUser("alice"),
                TelegramAction.IgnoreSubmission("t3_abc"),
                TelegramAction.DeleteComment("t1_xyz"),
                TelegramAction.OpenUrl("https://reddit.com/r/GhostAdventures/1"),
            ),
        )

        verify {
            telegramClient.execute(
                match<SendMessage> { message ->
                    val markup = message.replyMarkup as? InlineKeyboardMarkup ?: return@match false
                    markup.keyboard.size == 2 &&
                        markup.keyboard[0].map { it.callbackData } == listOf("iu:alice", "ip:t3_abc", "dc:t1_xyz") &&
                        markup.keyboard[1].map { it.url } == listOf("https://reddit.com/r/GhostAdventures/1")
                }
            )
        }
    }

    @Test
    fun testConsume_WhenStatusMessage_SendsStatusWithMetricsAndRefreshButton() {
        bot.consume(listOf(messageUpdate("/status")))

        verify {
            telegramClient.execute(
                match<SendMessage> { message ->
                    val markup = message.replyMarkup as? InlineKeyboardMarkup ?: return@match false
                    message.chatId == "123456" &&
                        message.text.contains("ZakBot Status") &&
                        message.text.contains("Gemini: `Enabled`") &&
                        message.text.contains("Quotes in DB: 150") &&
                        message.text.contains("Ignored users: 7") &&
                        message.text.contains("Ignored posts: 3") &&
                        markup.keyboard.single().single().callbackData == "rs"
                }
            )
        }
    }

    @Test
    fun testConsume_WhenStatusMessageAndGeminiApiKeyBlank_ShowsNoApiKey() {
        geminiProperties = GeminiProperties(apiKey = "", enabled = true)
        bot = createBot()

        bot.consume(listOf(messageUpdate("/status")))

        verify {
            telegramClient.execute(
                match<SendMessage> { message ->
                    message.text.contains("Gemini: `No API key`")
                }
            )
        }
    }

    @Test
    fun testConsume_WhenStatusMessageAndGeminiDisabled_ShowsDisabled() {
        geminiProperties = GeminiProperties(apiKey = "test-key", enabled = false)
        bot = createBot()

        bot.consume(listOf(messageUpdate("/status")))

        verify {
            telegramClient.execute(
                match<SendMessage> { message ->
                    message.text.contains("Gemini: `Disabled`")
                }
            )
        }
    }

    @Test
    fun testConsume_WhenStatusMessageAndGeminiDryRun_ShowsDryRun() {
        geminiProperties = GeminiProperties(apiKey = "test-key", enabled = true, dryRun = true)
        bot = createBot()

        bot.consume(listOf(messageUpdate("/status")))

        verify {
            telegramClient.execute(
                match<SendMessage> { message ->
                    message.text.contains("Gemini: `Dry run`")
                }
            )
        }
    }

    @Test
    fun testConsume_WhenUnrelatedMessage_DoesNothing() {
        bot.consume(listOf(messageUpdate("/help")))

        verify(exactly = 0) {
            telegramClient.execute(any<SendMessage>())
        }
    }

    @Test
    fun testConsume_WhenUnauthorizedChatId_DoesNothing() {
        bot.consume(listOf(messageUpdate("/status", chatId = 999999L)))
        bot.consume(listOf(callbackUpdate("iu:alice", chatId = 999999L)))

        verify(exactly = 0) {
            telegramClient.execute(any<SendMessage>())
            telegramClient.execute(any<AnswerCallbackQuery>())
        }
    }

    @Test
    fun testConsume_WhenIgnoreUserCallback_IgnoresUserAndTogglesButton() {
        val update = callbackUpdate(
            data = "iu:alice",
            buttons = listOf(
                InlineKeyboardRow(
                    InlineKeyboardButton.builder().text("🚫 Ignore User").callbackData("iu:alice").build(),
                    InlineKeyboardButton.builder().text("🚫 Ignore Post").callbackData("ip:t3_abc").build(),
                ),
                InlineKeyboardRow(
                    InlineKeyboardButton.builder().text("🔗 View on Reddit").url("https://reddit.com/1").build(),
                ),
            ),
        )

        bot.consume(listOf(update))

        verify {
            botCache.ignoreUser("alice", "telegram")
            telegramClient.execute(match<AnswerCallbackQuery> { it.callbackQueryId == "cb-1" && it.text == "Ignored u/alice" })
            telegramClient.execute(
                match<EditMessageReplyMarkup> { edit ->
                    val rows = edit.replyMarkup.keyboard
                    rows[0][0].callbackData == "uu:alice" &&
                        rows[0][0].text == "✅ Unignore User" &&
                        rows[0][1].callbackData == "ip:t3_abc" &&
                        rows[1][0].url == "https://reddit.com/1"
                }
            )
        }
    }

    @Test
    fun testConsume_WhenUnignoreUserCallback_UnignoresUserAndTogglesButton() {
        val update = callbackUpdate(
            data = "uu:alice",
            buttons = listOf(
                InlineKeyboardRow(
                    InlineKeyboardButton.builder().text("✅ Unignore User").callbackData("uu:alice").build(),
                ),
            ),
        )

        bot.consume(listOf(update))

        verify {
            botCache.unignoreUser("alice")
            telegramClient.execute(match<AnswerCallbackQuery> { it.text == "Unignored u/alice" })
            telegramClient.execute(
                match<EditMessageReplyMarkup> { edit ->
                    edit.replyMarkup.keyboard[0][0].callbackData == "iu:alice"
                }
            )
        }
    }

    @Test
    fun testConsume_WhenIgnorePostCallback_IgnoresPostAndTogglesButton() {
        val update = callbackUpdate(
            data = "ip:t3_abc",
            buttons = listOf(
                InlineKeyboardRow(
                    InlineKeyboardButton.builder().text("🚫 Ignore Post").callbackData("ip:t3_abc").build(),
                ),
            ),
        )

        bot.consume(listOf(update))

        verify {
            botCache.ignoreSubmission("t3_abc", "Ignored via Telegram")
            telegramClient.execute(match<AnswerCallbackQuery> { it.text == "Post ignored" })
            telegramClient.execute(
                match<EditMessageReplyMarkup> { edit ->
                    edit.replyMarkup.keyboard[0][0].callbackData == "up:t3_abc"
                }
            )
        }
    }

    @Test
    fun testConsume_WhenUnignorePostCallback_UnignoresPostAndTogglesButton() {
        val update = callbackUpdate(
            data = "up:t3_abc",
            buttons = listOf(
                InlineKeyboardRow(
                    InlineKeyboardButton.builder().text("✅ Unignore Post").callbackData("up:t3_abc").build(),
                ),
            ),
        )

        bot.consume(listOf(update))

        verify {
            botCache.unignoreSubmission("t3_abc")
            telegramClient.execute(match<AnswerCallbackQuery> { it.text == "Post unignored" })
            telegramClient.execute(
                match<EditMessageReplyMarkup> { edit ->
                    edit.replyMarkup.keyboard[0][0].callbackData == "ip:t3_abc"
                }
            )
        }
    }

    @Test
    fun testConsume_WhenDeleteCommentCallbackSucceeds_DeletesAndMarksDeleted() {
        every { redditService.deleteCommentById("t1_xyz") } returns true
        val update = callbackUpdate(
            data = "dc:t1_xyz",
            buttons = listOf(
                InlineKeyboardRow(
                    InlineKeyboardButton.builder().text("🗑️ Delete Reply").callbackData("dc:t1_xyz").build(),
                ),
            ),
        )

        bot.consume(listOf(update))

        verify {
            redditService.deleteCommentById("t1_xyz")
            telegramClient.execute(match<AnswerCallbackQuery> { it.text == "Reply deleted" })
            telegramClient.execute(
                match<EditMessageReplyMarkup> { edit ->
                    edit.replyMarkup.keyboard[0][0].callbackData == "cd" &&
                        edit.replyMarkup.keyboard[0][0].text == "✅ Deleted"
                }
            )
        }
    }

    @Test
    fun testConsume_WhenDeleteCommentCallbackFails_LeavesButtonUnchanged() {
        every { redditService.deleteCommentById("t1_xyz") } returns false
        val update = callbackUpdate(
            data = "dc:t1_xyz",
            buttons = listOf(
                InlineKeyboardRow(
                    InlineKeyboardButton.builder().text("🗑️ Delete Reply").callbackData("dc:t1_xyz").build(),
                ),
            ),
        )

        bot.consume(listOf(update))

        verify {
            redditService.deleteCommentById("t1_xyz")
            telegramClient.execute(match<AnswerCallbackQuery> { it.text == "Failed to delete reply" })
        }
        verify(exactly = 0) {
            telegramClient.execute(any<EditMessageReplyMarkup>())
        }
    }

    @Test
    fun testConsume_WhenRefreshStatusCallback_EditsStatusMessage() {
        val update = callbackUpdate(data = "rs")

        bot.consume(listOf(update))

        verify {
            telegramClient.execute(match<AnswerCallbackQuery> { it.text == "Status refreshed" })
            telegramClient.execute(
                match<EditMessageText> { edit ->
                    edit.chatId == "123456" &&
                        edit.messageId == 42 &&
                        edit.text.contains("ZakBot Status") &&
                        edit.replyMarkup.keyboard.single().single().callbackData == "rs"
                }
            )
        }
    }

    @Test
    fun testGetBotTokenAndConsumer() {
        assertEquals("dummy-token", bot.getBotToken())
        assertSame(bot, bot.getUpdatesConsumer())
    }

    @Test
    fun testConsume_WhenStatusMessageWithBotMention_SendsStatus() {
        bot.consume(listOf(messageUpdate("/status@zakbagansbot")))

        verify {
            telegramClient.execute(
                match<SendMessage> { message ->
                    message.text.contains("ZakBot Status")
                }
            )
        }
    }

    @Test
    fun testStatusMessage_WhenUptimeLessThanOneSecond_ShowsDefaultUptime() {
        every { applicationContext.startupDate } returns System.currentTimeMillis()

        bot.consume(listOf(messageUpdate("/status")))

        verify {
            telegramClient.execute(
                match<SendMessage> { message ->
                    message.text.contains("< 1 second")
                }
            )
        }
    }

    @Test
    fun testSendMessage_WhenTelegramClientThrows_CatchesAndDoesNotThrow() {
        every { telegramClient.execute(any<SendMessage>()) } throws RuntimeException("Telegram API unavailable")

        assertDoesNotThrow {
            bot.sendMessage("Test message")
        }
    }

    private fun createBot(): TelegramBot {
        val buildProperties = BuildProperties(Properties().apply {
            setProperty("version", "3.0.0")
        })
        return TelegramBot(
            applicationContext = applicationContext,
            phraseRepository = phraseRepository,
            ignoredUserRepository = ignoredUserRepository,
            ignoredSubmissionRepository = ignoredSubmissionRepository,
            botCache = botCache,
            redditService = redditService,
            telegramClient = telegramClient,
            buildProperties = buildProperties,
            telegramProperties = TelegramProperties(token = "dummy-token", chatId = "123456"),
            geminiProperties = geminiProperties,
        )
    }

    private fun messageUpdate(text: String, chatId: Long = 123456L): Update {
        val msg = mockk<Message> {
            every { this@mockk.chatId } returns chatId
            every { this@mockk.text } returns text
        }
        return mockk {
            every { callbackQuery } returns null
            every { message } returns msg
        }
    }

    private fun callbackUpdate(
        data: String,
        chatId: Long = 123456L,
        buttons: List<InlineKeyboardRow> = emptyList(),
    ): Update {
        val expectedChatId = chatId
        val msg = mockk<Message> {
            every { this@mockk.chatId } returns expectedChatId
            every { messageId } returns 42
            every { replyMarkup } returns InlineKeyboardMarkup.builder().keyboard(buttons).build()
        }
        val cb = mockk<CallbackQuery> {
            every { id } returns "cb-1"
            every { this@mockk.data } returns data
            every { message } returns msg
        }
        return mockk {
            every { callbackQuery } returns cb
            every { message } returns null
        }
    }
}
