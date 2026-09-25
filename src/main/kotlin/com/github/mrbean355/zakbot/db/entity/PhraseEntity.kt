package com.github.mrbean355.zakbot.db.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity(name = "phrase")
data class PhraseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Int,
    val content: String,
    val usages: Int,
    val type: Int,
    val source: String?,
)
