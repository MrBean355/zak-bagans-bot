package com.github.mrbean355.zakbot.db.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.Instant

@Entity(name = "ignored_submission")
data class IgnoredSubmissionEntity(
    @Id val fullName: String,
    val since: Instant,
    val reason: String?,
)
