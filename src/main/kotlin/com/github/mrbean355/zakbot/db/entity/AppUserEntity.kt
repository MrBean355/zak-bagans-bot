package com.github.mrbean355.zakbot.db.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "app_user")
class AppUserEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val username: String,
    var password: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppUserEntity) return false
        return if (id != 0L && other.id != 0L) {
            id == other.id
        } else {
            username == other.username
        }
    }

    override fun hashCode(): Int = username.hashCode()

    override fun toString(): String = "AppUserEntity(id=$id, username='$username')"
}
