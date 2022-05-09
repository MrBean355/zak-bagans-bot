package com.github.mrbean355.zakbot.util

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SystemClockTest {

    @Test
    internal fun testCurrentTimeMillis() {
        val result = SystemClock().currentTimeMillis

        assertTrue(System.currentTimeMillis() - result < 100)
    }
}