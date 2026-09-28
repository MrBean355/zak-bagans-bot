package com.github.mrbean355.zakbot.db.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.Instant

@Entity(name = "ignored_user")
class IgnoredUserEntity(
    @Id val userId: String,
    val since: Instant,
    val source: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IgnoredUserEntity) return false
        return userId == other.userId
    }

    override fun hashCode(): Int = userId.hashCode()

    override fun toString(): String = "IgnoredUserEntity(userId='$userId', since=$since, source='$source')"
}
