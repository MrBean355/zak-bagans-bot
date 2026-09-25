package com.github.mrbean355.zakbot.db.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.Date

@Entity(name = "last_checked")
data class LastCheckedEntity(
    @Id val key: String,
    val value: Date,
)
