package com.github.mrbean355.zakbot.db.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.Instant

@Entity(name = "ignored_user")
data class IgnoredUserEntity(
    @Id val userId: String,
    val since: Instant,
    val source: String,
)
