package com.github.mrbean355.zakbot.db.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.Instant

@Entity(name = "last_checked")
class LastCheckedEntity(
    @Id val key: String,
    val value: Instant,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LastCheckedEntity) return false
        return key == other.key
    }

    override fun hashCode(): Int = key.hashCode()

    override fun toString(): String = "LastCheckedEntity(key='$key', value=$value)"
}
