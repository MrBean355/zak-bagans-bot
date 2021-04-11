package com.github.mrbean355.zakbot

import com.github.mrbean355.zakbot.util.SystemClock
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

internal class CacheTest {
    @MockK
    private lateinit var systemClock: SystemClock

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        every { systemClock.currentTimeMillis } returns 1_000_000
    }

    @AfterEach
    internal fun tearDown() {
        File("cache.json").delete()
    }

    @Test
    internal fun testConstruction_FileDoesNotExist_InitialisesLastCheckedToCurrentTime() {
        val cache = Cache(systemClock)

        val actual = cache.getLastChecked()

        assertEquals(1_000_000, actual)
    }

    @Test
    internal fun testConstruction_FileAlreadyExists_ReturnsCachedCurrentTime() {
        File("cache.json").writeText("{\"lastChecked\":987654321}")
        val cache = Cache(systemClock)

        val actual = cache.getLastChecked()

        assertEquals(987654321, actual)
    }

    @Test
    internal fun testSetLastChecked_UpdatesCache() {
        val cache = Cache(systemClock)

        cache.setLastChecked(123456789)

        assertEquals("{\"lastChecked\":123456789}", File("cache.json").readText())
    }
}