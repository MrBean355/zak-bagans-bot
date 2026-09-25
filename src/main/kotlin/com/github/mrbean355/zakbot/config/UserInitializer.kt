package com.github.mrbean355.zakbot.config

import com.github.mrbean355.zakbot.db.entity.AppUserEntity
import com.github.mrbean355.zakbot.db.repo.AppUserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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
    @Value($$"${ADMIN_USERNAME:admin}") private val adminUsername: String,
    @Value($$"${ADMIN_PASSWORD:}") private val adminPassword: String,
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(UserInitializer::class.java)

    override fun run(vararg args: String) {
        val username = adminUsername.ifBlank { "admin" }
        val password = adminPassword.ifBlank { null }

        if (password != null) {
            createOrUpdateUser(username, password)
            logger.info("Admin user configured: $username")
        } else if (environment.matchesProfiles("dev") && appUserRepository.count() == 0L) {
            createOrUpdateUser(username, "password")
            logger.warn("Dev profile active: default admin user created ($username / password)")
        } else if (appUserRepository.count() == 0L) {
            logger.warn("No ADMIN_PASSWORD provided and no users exist. Skipping admin account creation.")
        }
    }

    @Transactional
    fun createOrUpdateUser(username: String, password: String) {
        val user = appUserRepository.findByUsername(username)
            ?: AppUserEntity(username = username, password = "")

        val encoded = passwordEncoder.encode(password) ?: return

        appUserRepository.save(user.copy(password = encoded))
    }
}
