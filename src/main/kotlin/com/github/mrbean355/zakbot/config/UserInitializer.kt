package com.github.mrbean355.zakbot.config

import com.github.mrbean355.zakbot.db.entity.AppUserEntity
import com.github.mrbean355.zakbot.db.repo.AppUserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional

@Configuration
open class UserInitializer(
    private val appUserRepository: AppUserRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value($$"${ADMIN_USERNAME:admin}") private val adminUsername: String,
    @Value($$"${ADMIN_PASSWORD:}") private val adminPassword: String,
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(UserInitializer::class.java)

    override fun run(vararg args: String) {
        val username = adminUsername.ifBlank { "admin" }
        val password = adminPassword.ifBlank { null }

        if (password != null) {
            createOrUpdateUser(username, password)
            logger.info("Admin user updated.")
        } else if (appUserRepository.count() == 0L) {
            createOrUpdateUser(username, "password")
            logger.info("Default admin user created: $username / password")
        }
    }

    @Transactional
    open fun createOrUpdateUser(username: String, password: String) {
        val user = appUserRepository.findByUsername(username)
            ?: AppUserEntity(username = username, password = "")

        val encoded = passwordEncoder.encode(password) ?: return

        appUserRepository.save(user.copy(password = encoded))
    }
}
