package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.db.entity.AppUserEntity
import com.github.mrbean355.zakbot.db.repo.AppUserRepository
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.userdetails.UsernameNotFoundException

class CustomUserDetailsServiceTest {

    @MockK
    private lateinit var appUserRepository: AppUserRepository

    private lateinit var service: CustomUserDetailsService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        service = CustomUserDetailsService(appUserRepository)
    }

    @Test
    fun testLoadUserByUsername_WhenUserExists_ReturnsUserDetails() {
        val userEntity = AppUserEntity(
            id = 1,
            username = "admin",
            password = "encoded-password",
        )
        every { appUserRepository.findByUsername("admin") } returns userEntity

        val userDetails = service.loadUserByUsername("admin")

        assertEquals("admin", userDetails.username)
        assertEquals("encoded-password", userDetails.password)
        assertTrue(userDetails.authorities.any { it.authority == "ROLE_ADMIN" })
    }

    @Test
    fun testLoadUserByUsername_WhenUserNotFound_ThrowsUsernameNotFoundException() {
        every { appUserRepository.findByUsername("unknown") } returns null

        val exception = assertThrows<UsernameNotFoundException> {
            service.loadUserByUsername("unknown")
        }

        assertEquals("User not found: unknown", exception.message)
    }
}
