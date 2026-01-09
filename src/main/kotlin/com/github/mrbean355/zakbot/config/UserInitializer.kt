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

    override fun run(vararg args: String?) {
        if (appUserRepository.count() == 0L) {
            val defaultAdmin = AppUserEntity(
                username = "admin",
                password = passwordEncoder.encode("password")
            )
            appUserRepository.save(defaultAdmin)
            logger.info("Default admin user created: admin / password")
        }
    }
}
