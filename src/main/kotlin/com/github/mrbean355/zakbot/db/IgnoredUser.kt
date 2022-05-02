package com.github.mrbean355.zakbot.db

import org.springframework.data.repository.CrudRepository
import java.util.Date
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class IgnoredUser(
    @Id val userId: String,
    val since: Date,
    val source: String
) {
    constructor() : this("", Date(), "")
}

interface IgnoredUserRepository : CrudRepository<IgnoredUser, String>
