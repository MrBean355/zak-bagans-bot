package com.github.mrbean355.zakbot.config

import com.github.mrbean355.zakbot.db.entity.AppUserEntity
import com.github.mrbean355.zakbot.db.repo.AppUserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
open class UserInitializer(
    private val appUserRepository: AppUserRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(UserInitializer::class.java)

    override fun run(vararg args: String) {
        val username = System.getenv("ADMIN_USERNAME").takeIf { !it.isNullOrBlank() } ?: "admin"
        val password = System.getenv("ADMIN_PASSWORD").takeIf { !it.isNullOrBlank() }

        if (password != null) {
            createOrUpdateUser(username, password)
            logger.info("Admin user updated.")
        } else if (appUserRepository.count() == 0L) {
            createOrUpdateUser(username, "password")
            logger.info("Default admin user created: $username / password")
        }
    }

    private fun createOrUpdateUser(username: String, password: String) {
        val user = appUserRepository.findByUsername(username)
            ?: AppUserEntity(username = username, password = "")

        val encoded = passwordEncoder.encode(password) ?: return

        appUserRepository.save(user.copy(password = encoded))
    }
}
