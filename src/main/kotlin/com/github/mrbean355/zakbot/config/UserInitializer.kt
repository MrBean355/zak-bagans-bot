package com.github.mrbean355.zakbot.config

import com.github.mrbean355.zakbot.db.entity.AppUserEntity
import com.github.mrbean355.zakbot.db.repo.AppUserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.env.Environment
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class UserInitializer(
    private val appUserRepository: AppUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val environment: Environment,
    private val adminProperties: AdminProperties,
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(UserInitializer::class.java)

    override fun run(vararg args: String) {
        val username = adminProperties.username.ifBlank { "admin" }
        val password = adminProperties.password.ifBlank { null }

        when {
            password != null -> {
                createOrUpdateUser(username, password)
                logger.info("Admin user configured: $username")
            }
            environment.matchesProfiles("dev") && appUserRepository.count() == 0L -> {
                createOrUpdateUser(username, "password")
                logger.warn("Dev profile active: default admin user created ($username / password)")
            }
            appUserRepository.count() == 0L -> {
                logger.warn("No ADMIN_PASSWORD provided and no users exist. Skipping admin account creation.")
            }
        }
    }

    @Transactional
    fun createOrUpdateUser(username: String, password: String) {
        val encoded = passwordEncoder.encode(password) ?: return
        val user = appUserRepository.findByUsername(username)
        if (user != null) {
            user.password = encoded
            appUserRepository.save(user)
        } else {
            appUserRepository.save(AppUserEntity(username = username, password = encoded))
        }
    }
}
