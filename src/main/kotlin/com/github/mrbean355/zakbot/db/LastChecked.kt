package com.github.mrbean355.zakbot.db

import org.springframework.data.repository.CrudRepository
import java.util.Date
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Temporal
import javax.persistence.TemporalType

@Entity
data class LastChecked(
    @Id val key: String,
    @Temporal(TemporalType.TIMESTAMP) val value: Date
) {
    constructor() : this("", Date(0))
}

interface LastCheckedRepository : CrudRepository<LastChecked, String>
