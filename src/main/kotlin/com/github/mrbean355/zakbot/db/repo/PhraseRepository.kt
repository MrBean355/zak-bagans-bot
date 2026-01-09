package com.github.mrbean355.zakbot.db.repo

import com.github.mrbean355.zakbot.db.entity.PhraseEntity
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface PhraseRepository : CrudRepository<PhraseEntity, Int> {

    fun findByType(type: Int): List<PhraseEntity>

    @Query("SELECT MIN(p.usages) FROM phrase p WHERE p.type = :type")
    fun findMinUsagesByType(type: Int): Int?

}
