package com.github.mrbean355.zakbot.db.entity

import com.github.mrbean355.zakbot.db.PhraseType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity(name = "phrase")
class PhraseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,
    val content: String,
    var usages: Int,
    val type: PhraseType,
    val source: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhraseEntity) return false
        return if (id != 0 && other.id != 0) {
            id == other.id
        } else {
            content == other.content &&
                usages == other.usages &&
                type == other.type &&
                source == other.source
        }
    }

    override fun hashCode(): Int = if (id != 0) id.hashCode() else content.hashCode()

    override fun toString(): String =
        "PhraseEntity(id=$id, content='$content', usages=$usages, type=$type, source=$source)"
}
