package com.github.mrbean355.zakbot.db.entity

import java.util.Date
import javax.persistence.Entity
import javax.persistence.Id

@Entity(name = "ignored_user")
data class IgnoredUserEntity(
    @Id val userId: String,
    val since: Date,
    val source: String
) {
    constructor() : this("", Date(), "")
}
