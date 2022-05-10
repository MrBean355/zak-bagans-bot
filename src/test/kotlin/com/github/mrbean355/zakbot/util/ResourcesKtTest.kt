package com.github.mrbean355.zakbot.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ResourcesKtTest {

    @Test
    internal fun testReadResourceFileLines() {
        val result = readResourceFileLines("test.txt")

        assertEquals(4, result.size)
        assertEquals("Now I need to ponder my existence and ask myself if I'm truly real.", result[0])
        assertEquals("She found it strange that people use their cellphones to actually talk to one another.", result[1])
        assertEquals("He said he was not there yesterday; however, many people saw him there.", result[2])
        assertEquals("I trust everything that's written in purple ink.", result[3])
    }
}