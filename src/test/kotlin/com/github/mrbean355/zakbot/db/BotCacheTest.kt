package com.github.mrbean355.zakbot.db

import com.github.mrbean355.zakbot.db.entity.IgnoredSubmissionEntity
import com.github.mrbean355.zakbot.db.entity.IgnoredUserEntity
import com.github.mrbean355.zakbot.db.entity.LastCheckedEntity
import com.github.mrbean355.zakbot.db.repo.IgnoredSubmissionRepository
import com.github.mrbean355.zakbot.db.repo.IgnoredUserRepository
import com.github.mrbean355.zakbot.db.repo.LastCheckedRepository
import com.github.mrbean355.zakbot.util.SystemClock
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.Optional

private const val CurrentTime = 1_000_000L

internal class BotCacheTest {
    @MockK
    private lateinit var lastCheckedRepository: LastCheckedRepository

    @MockK
    private lateinit var ignoredUserRepository: IgnoredUserRepository

    @MockK
    private lateinit var ignoredSubmissionRepository: IgnoredSubmissionRepository

    @MockK
    private lateinit var systemClock: SystemClock
    private lateinit var botCache: BotCache

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        every { systemClock.currentTimeMillis } returns CurrentTime
        botCache = BotCache(lastCheckedRepository, ignoredUserRepository, ignoredSubmissionRepository, systemClock)
    }

    @Test
    internal fun testGetLastSubmissionTime_EntityPresent_ReturnsEntityValue() {
        val lastChecked = mockk<Date>()
        every { lastCheckedRepository.findById("post") } returns Optional.of(LastCheckedEntity("post", lastChecked))

        val result = botCache.getLastSubmissionTime()

        assertSame(lastChecked, result)
        verify(inverse = true) { lastCheckedRepository.save(any()) }
    }

    @Test
    internal fun testGetLastSubmissionTime_EntityNotPresent_SavesEntityAndReturnsValue() {
        every { lastCheckedRepository.findById("post") } returns Optional.empty()
        val lastChecked = mockk<Date>()
        every { lastCheckedRepository.save(any()) } returns LastCheckedEntity("post", lastChecked)

        val result = botCache.getLastSubmissionTime()

        assertSame(lastChecked, result)
        val slot = slot<LastCheckedEntity>()
        verify { lastCheckedRepository.save(capture(slot)) }
        assertEquals("post", slot.captured.key)
        assertEquals(CurrentTime, slot.captured.value.time)
    }

    @Test
    internal fun testSetLastSubmissionTime_SavesEntity() {
        every { lastCheckedRepository.save(any()) } returns mockk()
        val lastChecked = mockk<Date>()

        botCache.setLastSubmissionTime(lastChecked)

        val slot = slot<LastCheckedEntity>()
        verify { lastCheckedRepository.save(capture(slot)) }
        assertEquals("post", slot.captured.key)
        assertSame(lastChecked, slot.captured.value)
    }

    @Test
    internal fun testGetLastCommentTime_EntityPresent_ReturnsEntityValue() {
        val lastChecked = mockk<Date>()
        every { lastCheckedRepository.findById("comment") } returns Optional.of(LastCheckedEntity("comment", lastChecked))

        val result = botCache.getLastCommentTime()

        assertSame(lastChecked, result)
        verify(inverse = true) { lastCheckedRepository.save(any()) }
    }

    @Test
    internal fun testGetLastCommentTime_EntityNotPresent_SavesEntityAndReturnsValue() {
        every { lastCheckedRepository.findById("comment") } returns Optional.empty()
        val lastChecked = mockk<Date>()
        every { lastCheckedRepository.save(any()) } returns LastCheckedEntity("comment", lastChecked)

        val result = botCache.getLastCommentTime()

        assertSame(lastChecked, result)
        val slot = slot<LastCheckedEntity>()
        verify { lastCheckedRepository.save(capture(slot)) }
        assertEquals("comment", slot.captured.key)
        assertEquals(CurrentTime, slot.captured.value.time)
    }

    @Test
    internal fun testSetLastCommentTime_SavesEntity() {
        every { lastCheckedRepository.save(any()) } returns mockk()
        val lastChecked = mockk<Date>()

        botCache.setLastCommentTime(lastChecked)

        val slot = slot<LastCheckedEntity>()
        verify { lastCheckedRepository.save(capture(slot)) }
        assertEquals("comment", slot.captured.key)
        assertSame(lastChecked, slot.captured.value)
    }

    @Test
    internal fun testIsUserIgnored_EntityPresent_ReturnsTrue() {
        every { ignoredUserRepository.findById("123") } returns Optional.of(mockk())

        val result = botCache.isUserIgnored("123")

        assertTrue(result)
    }

    @Test
    internal fun testIsUserIgnored_EntityNotPresent_ReturnsFalse() {
        every { ignoredUserRepository.findById("123") } returns Optional.empty()

        val result = botCache.isUserIgnored("123")

        assertFalse(result)
    }

    @Test
    internal fun testIgnoreUser_SavesEntity() {
        every { ignoredUserRepository.save(any()) } returns mockk()

        botCache.ignoreUser("123", "somewhere")

        val slot = slot<IgnoredUserEntity>()
        verify { ignoredUserRepository.save(capture(slot)) }
        assertEquals("123", slot.captured.userId)
        assertEquals("somewhere", slot.captured.source)
        assertEquals(CurrentTime, slot.captured.since.time)
    }

    @Test
    internal fun testIsSubmissionIgnored_EntityPresent_ReturnsTrue() {
        every { ignoredSubmissionRepository.findById("123") } returns Optional.of(mockk())

        val result = botCache.isSubmissionIgnored("123")

        assertTrue(result)
    }

    @Test
    internal fun testIsSubmissionIgnored_EntityNotPresent_ReturnsFalse() {
        every { ignoredSubmissionRepository.findById("123") } returns Optional.empty()

        val result = botCache.isSubmissionIgnored("123")

        assertFalse(result)
    }

    @Test
    internal fun testIgnoreSubmission_SavesEntity() {
        every { ignoredSubmissionRepository.save(any()) } returns mockk()

        botCache.ignoreSubmission("123", "Sensitive topic")

        val slot = slot<IgnoredSubmissionEntity>()
        verify { ignoredSubmissionRepository.save(capture(slot)) }
        assertEquals("123", slot.captured.fullName)
        assertEquals(CurrentTime, slot.captured.since.time)
        assertEquals("Sensitive topic", slot.captured.reason)
    }
}