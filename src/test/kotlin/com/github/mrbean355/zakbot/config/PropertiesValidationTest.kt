package com.github.mrbean355.zakbot.config

import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PropertiesValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun testDefaults_AreValid() {
        assertTrue(validator.validate(AdminProperties()).isEmpty())
        assertTrue(validator.validate(GeminiProperties()).isEmpty())
        assertTrue(validator.validate(ZakbotProperties()).isEmpty())
        assertTrue(validator.validate(RedditProperties(accountPassword = "pass", clientSecret = "secret")).isEmpty())
        assertTrue(validator.validate(TelegramProperties(token = "token", chatId = "123")).isEmpty())
    }

    @Test
    fun testAdminProperties_BlankUsername_FailsValidation() {
        val violations = validator.validate(AdminProperties(username = " "))

        assertFalse(violations.isEmpty())
    }

    @Test
    fun testGeminiProperties_BlankModel_FailsValidation() {
        val violations = validator.validate(GeminiProperties(model = ""))

        assertFalse(violations.isEmpty())
    }

    @Test
    fun testZakbotProperties_PositiveDeleteThreshold_FailsValidation() {
        val properties = ZakbotProperties(downvotes = ZakbotProperties.Downvotes(deleteThreshold = 1))

        val violations = validator.validate(properties)

        assertFalse(violations.isEmpty())
    }

    @Test
    fun testZakbotProperties_OutOfRangeSensitivityThreshold_FailsValidation() {
        val belowZero = ZakbotProperties(moderation = ZakbotProperties.Moderation(sensitivityThreshold = -0.1f))
        val aboveOne = ZakbotProperties(moderation = ZakbotProperties.Moderation(sensitivityThreshold = 1.1f))

        assertFalse(validator.validate(belowZero).isEmpty())
        assertFalse(validator.validate(aboveOne).isEmpty())
    }

    @Test
    fun testRedditProperties_BlankFields_FailValidation() {
        assertFalse(validator.validate(RedditProperties(accountPassword = "", clientSecret = "secret")).isEmpty())
        assertFalse(validator.validate(RedditProperties(accountPassword = "pass", clientSecret = " ")).isEmpty())
    }

    @Test
    fun testTelegramProperties_BlankFields_FailValidation() {
        assertFalse(validator.validate(TelegramProperties(token = "", chatId = "123")).isEmpty())
        assertFalse(validator.validate(TelegramProperties(token = "token", chatId = " ")).isEmpty())
    }
}
