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
    internal fun testConstruction_FileDoesNotExist_InitialisesFieldsToCurrentTime() {
        val cache = Cache(systemClock)

        assertEquals(1_000_000, cache.getLastPost())
        assertEquals(1_000_000, cache.getLastComment())
    }

    @Test
    internal fun testConstruction_FileAlreadyExists_ReturnsCachedCurrentTime() {
        File("cache.json").writeText("{\"lastPost\": 111,\"lastComment\":222}")
        val cache = Cache(systemClock)

        assertEquals(111, cache.getLastPost())
        assertEquals(222, cache.getLastComment())
    }

    @Test
    internal fun testSetLastPost_UpdatesCache() {
        val cache = Cache(systemClock)

        cache.setLastPost(111)

        assertEquals("{\"lastPost\":111,\"lastComment\":1000000}", File("cache.json").readText())
    }

    @Test
    internal fun testSetLastComment_UpdatesCache() {
        val cache = Cache(systemClock)

        cache.setLastComment(222)

        assertEquals("{\"lastPost\":1000000,\"lastComment\":222}", File("cache.json").readText())
    }
}