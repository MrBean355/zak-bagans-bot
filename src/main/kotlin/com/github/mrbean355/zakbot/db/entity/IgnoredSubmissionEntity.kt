package com.github.mrbean355.zakbot.db.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.Date

@Entity(name = "ignored_submission")
data class IgnoredSubmissionEntity(
    @Id val fullName: String,
    val since: Date,
    val reason: String?,
) {
    constructor() : this("", Date(), null)
}
