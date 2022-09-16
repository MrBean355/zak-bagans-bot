package com.github.mrbean355.zakbot.db.entity

import java.util.Date
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Temporal
import javax.persistence.TemporalType

@Entity(name = "last_checked")
data class LastCheckedEntity(
    @Id val key: String,
    @Temporal(TemporalType.TIMESTAMP) val value: Date
) {
    constructor() : this("", Date(0))
}
