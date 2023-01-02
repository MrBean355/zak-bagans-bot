package com.github.mrbean355.zakbot.db.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import java.util.Date

@Entity(name = "last_checked")
data class LastCheckedEntity(
    @Id val key: String,
    @Temporal(TemporalType.TIMESTAMP) val value: Date
) {
    constructor() : this("", Date(0))
}
