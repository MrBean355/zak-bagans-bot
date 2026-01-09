package com.github.mrbean355.zakbot.db.repo

import com.github.mrbean355.zakbot.db.entity.AppUserEntity
import org.springframework.data.repository.CrudRepository

interface AppUserRepository : CrudRepository<AppUserEntity, Long> {
    fun findByUsername(username: String): AppUserEntity?
}
