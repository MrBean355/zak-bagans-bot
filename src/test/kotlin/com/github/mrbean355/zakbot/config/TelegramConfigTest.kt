package com.github.mrbean355.zakbot.config

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class TelegramConfigTest {

    @Test
    fun testTelegramClient_InstantiatesClient() {
        val config = TelegramConfig()
        val client = config.telegramClient(TelegramProperties(token = "dummy-token"))
        assertNotNull(client)
    }
}
