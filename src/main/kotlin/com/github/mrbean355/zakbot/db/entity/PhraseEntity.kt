package com.github.mrbean355.zakbot.db.entity

import com.github.mrbean355.zakbot.db.PhraseType
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
) {

    constructor() : this(0, "", 0, PhraseType.Generic, null)

}
