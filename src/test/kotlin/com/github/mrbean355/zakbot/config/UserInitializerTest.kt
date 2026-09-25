package com.github.mrbean355.zakbot.config

import com.github.mrbean355.zakbot.db.entity.AppUserEntity
import com.github.mrbean355.zakbot.db.repo.AppUserRepository
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.env.Environment
import org.springframework.security.crypto.password.PasswordEncoder

class UserInitializerTest {
    @MockK
    private lateinit var appUserRepository: AppUserRepository

    @MockK
    private lateinit var passwordEncoder: PasswordEncoder

    @MockK
    private lateinit var environment: Environment

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        every { passwordEncoder.encode(any()) } answers { "encoded_${firstArg<String>()}" }
    }

    @Test
    fun testRun_WhenPasswordProvided_CreatesOrUpdatesUser() {
        every { appUserRepository.findByUsername("admin") } returns null
        every { appUserRepository.save(any<AppUserEntity>()) } answers { firstArg() }

        val initializer = UserInitializer(appUserRepository, passwordEncoder, environment, "admin", "secret123")
        initializer.run()

        val slot = slot<AppUserEntity>()
        verify { appUserRepository.save(capture(slot)) }
        assertEquals("admin", slot.captured.username)
        assertEquals("encoded_secret123", slot.captured.password)
    }

    @Test
    fun testRun_WhenNoPasswordAndDevProfileAndNoUsers_CreatesDefaultUser() {
        every { environment.matchesProfiles("dev") } returns true
        every { appUserRepository.count() } returns 0L
        every { appUserRepository.findByUsername("admin") } returns null
        every { appUserRepository.save(any<AppUserEntity>()) } answers { firstArg() }

        val initializer = UserInitializer(appUserRepository, passwordEncoder, environment, "admin", "")
        initializer.run()

        val slot = slot<AppUserEntity>()
        verify { appUserRepository.save(capture(slot)) }
        assertEquals("admin", slot.captured.username)
        assertEquals("encoded_password", slot.captured.password)
    }

    @Test
    fun testRun_WhenNoPasswordAndProdProfileAndNoUsers_DoesNotCreateUser() {
        every { environment.matchesProfiles("dev") } returns false
        every { appUserRepository.count() } returns 0L

        val initializer = UserInitializer(appUserRepository, passwordEncoder, environment, "admin", "")
        initializer.run()

        verify(inverse = true) { appUserRepository.save(any()) }
    }
}
