package com.github.mrbean355.zakbot

import com.github.mrbean355.zakbot.db.BotCache
import com.github.mrbean355.zakbot.reddit.model.Comment
import com.github.mrbean355.zakbot.reddit.model.RedditFlair
import com.github.mrbean355.zakbot.reddit.model.Submission
import com.github.mrbean355.zakbot.service.ContributionService
import com.github.mrbean355.zakbot.service.RedditService
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class ZakBagansBotTest {

    @MockK
    private lateinit var redditService: RedditService

    @MockK
    private lateinit var botCache: BotCache

    @MockK
    private lateinit var contributionService: ContributionService

    private lateinit var bot: ZakBagansBot

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        bot = ZakBagansBot(redditService, botCache, contributionService)
    }

    @Test
    fun testCheckContributions_WhenSubmissionsAndCommentsFound_ProcessesAndUpdatesTimestamps() {
        val lastSubmissionTime = Instant.ofEpochMilli(1000L)
        val lastCommentTime = Instant.ofEpochMilli(2000L)
        val subTime1 = Instant.ofEpochMilli(1500L)
        val subTime2 = Instant.ofEpochMilli(1200L)
        val comTime1 = Instant.ofEpochMilli(2500L)

        val submission1 = mockk<Submission> { every { created } returns subTime1 }
        val submission2 = mockk<Submission> { every { created } returns subTime2 }
        val comment1 = mockk<Comment> { every { created } returns comTime1 }

        every { botCache.getLastSubmissionTime() } returns lastSubmissionTime
        every { botCache.getLastCommentTime() } returns lastCommentTime
        every { redditService.getSubmissionsSince(lastSubmissionTime) } returns listOf(submission1, submission2)
        every { redditService.getCommentsSince(lastCommentTime) } returns listOf(comment1)
        justRun { botCache.setLastSubmissionTime(any()) }
        justRun { botCache.setLastCommentTime(any()) }
        justRun { contributionService.processSubmission(any()) }
        justRun { contributionService.processComment(any()) }

        bot.checkContributions()

        verify(ordering = io.mockk.Ordering.ORDERED) {
            botCache.setLastSubmissionTime(subTime1)
            contributionService.processSubmission(submission1)
            contributionService.processSubmission(submission2)
            botCache.setLastCommentTime(comTime1)
            contributionService.processComment(comment1)
        }
    }

    @Test
    fun testCheckContributions_WhenNoNewContributions_DoesNotUpdateTimestampsOrProcess() {
        val lastSubmissionTime = Instant.ofEpochMilli(1000L)
        val lastCommentTime = Instant.ofEpochMilli(2000L)

        every { botCache.getLastSubmissionTime() } returns lastSubmissionTime
        every { botCache.getLastCommentTime() } returns lastCommentTime
        every { redditService.getSubmissionsSince(lastSubmissionTime) } returns emptyList()
        every { redditService.getCommentsSince(lastCommentTime) } returns emptyList()

        bot.checkContributions()

        verify(exactly = 0) {
            botCache.setLastSubmissionTime(any())
            botCache.setLastCommentTime(any())
            contributionService.processSubmission(any())
            contributionService.processComment(any())
        }
    }

    @Test
    fun testUpdateBotFlair_WhenOptionsAvailable_SetsRandomFlair() {
        val flair1 = RedditFlair("id1", "Ghost Hunter")
        val flair2 = RedditFlair("id2", "Zak Bagans")
        every { redditService.getFlairOptions() } returns listOf(flair1, flair2)
        justRun { redditService.setBotFlair(any()) }

        bot.updateBotFlair()

        verify(exactly = 1) {
            redditService.setBotFlair(match { it == flair1 || it == flair2 })
        }
    }

    @Test
    fun testUpdateBotFlair_WhenNoOptionsAvailable_DoesNotSetFlair() {
        every { redditService.getFlairOptions() } returns emptyList()

        bot.updateBotFlair()

        verify(exactly = 0) {
            redditService.setBotFlair(any())
        }
    }
}
