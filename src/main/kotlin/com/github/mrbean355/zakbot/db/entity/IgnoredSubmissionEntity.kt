package com.github.mrbean355.zakbot.db.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.Instant

@Entity(name = "ignored_submission")
class IgnoredSubmissionEntity(
    @Id val fullName: String,
    val since: Instant,
    val reason: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IgnoredSubmissionEntity) return false
        return fullName == other.fullName
    }

    override fun hashCode(): Int = fullName.hashCode()

    override fun toString(): String = "IgnoredSubmissionEntity(fullName='$fullName', since=$since, reason=$reason)"
}
