package com.github.mrbean355.zakbot.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MessagesKtTest {

    @Test
    internal fun testCountOccurrences() {
        val input = "He found his art never progressed when he literally used his sweat and tears."

        assertEquals(1, input.countOccurrences("he"))
        assertEquals(2, input.countOccurrences("his"))
        assertEquals(1, input.countOccurrences("tears"))
    }
}