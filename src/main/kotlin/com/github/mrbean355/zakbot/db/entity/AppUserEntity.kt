package com.github.mrbean355.zakbot.db.entity

import jakarta.persistence.*

@Entity
@Table(name = "app_user")
data class AppUserEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val username: String,
    val password: String
) {
    constructor() : this(0, "", "")
}
