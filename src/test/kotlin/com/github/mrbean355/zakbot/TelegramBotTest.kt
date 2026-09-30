package com.github.mrbean355.zakbot

import com.github.mrbean355.zakbot.config.TelegramProperties
import com.github.mrbean355.zakbot.db.repo.IgnoredSubmissionRepository
import com.github.mrbean355.zakbot.db.repo.IgnoredUserRepository
import com.github.mrbean355.zakbot.db.repo.PhraseRepository
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.info.BuildProperties
import org.springframework.context.ApplicationContext
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
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
    private lateinit var telegramClient: TelegramClient

    private lateinit var bot: TelegramBot

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { applicationContext.startupDate } returns System.currentTimeMillis() - 3600_000L
        every { phraseRepository.count() } returns 150L
        every { ignoredUserRepository.count() } returns 7L
        every { ignoredSubmissionRepository.count() } returns 3L
        every { telegramClient.execute(any<SendMessage>()) } returns mockk()

        val buildProperties = BuildProperties(Properties().apply {
            setProperty("version", "3.0.0")
        })

        bot = TelegramBot(
            applicationContext = applicationContext,
            phraseRepository = phraseRepository,
            ignoredUserRepository = ignoredUserRepository,
            ignoredSubmissionRepository = ignoredSubmissionRepository,
            telegramClient = telegramClient,
            buildProperties = buildProperties,
            telegramProperties = TelegramProperties(
                token = "dummy-token",
                chatId = "123456",
            ),
        )
    }

    @Test
    fun testSendMessage_SendsToConfiguredChatId() {
        bot.sendMessage("Hello test")

        verify {
            telegramClient.execute(
                match<SendMessage> { message ->
                    message.chatId == "123456" && message.text == "Hello test"
                }
            )
        }
    }

    @Test
    fun testConsume_WhenStatusMessage_SendsStatusWithMetrics() {
        val update = mockk<Update> {
            every { message } returns mockk {
                every { text } returns "/status"
            }
        }

        bot.consume(listOf(update))

        verify {
            telegramClient.execute(
                match<SendMessage> { message ->
                    message.chatId == "123456" &&
                        message.text.contains("ZakBot Status") &&
                        message.text.contains("Quotes in DB: 150") &&
                        message.text.contains("Ignored users: 7") &&
                        message.text.contains("Ignored posts: 3")
                }
            )
        }
    }

    @Test
    fun testConsume_WhenUnrelatedMessage_DoesNothing() {
        val update = mockk<Update> {
            every { message } returns mockk {
                every { text } returns "/help"
            }
        }

        bot.consume(listOf(update))

        verify(exactly = 0) {
            telegramClient.execute(any<SendMessage>())
        }
    }

    @Test
    fun testGetBotTokenAndConsumer() {
        org.junit.jupiter.api.Assertions.assertEquals("dummy-token", bot.getBotToken())
        org.junit.jupiter.api.Assertions.assertSame(bot, bot.getUpdatesConsumer())
    }

    @Test
    fun testConsume_WhenStatusMessageWithBotMention_SendsStatus() {
        val update = mockk<Update> {
            every { message } returns mockk {
                every { text } returns "/status@zakbagansbot"
            }
        }

        bot.consume(listOf(update))

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

        val update = mockk<Update> {
            every { message } returns mockk {
                every { text } returns "/status"
            }
        }

        bot.consume(listOf(update))

        verify {
            telegramClient.execute(
                match<SendMessage> { message ->
                    message.text.contains("< 1 second")
                }
            )
        }
    }
}
