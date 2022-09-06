package com.github.mrbean355.zakbot.db.repo

import com.github.mrbean355.zakbot.db.entity.IgnoredUserEntity
import org.springframework.data.repository.CrudRepository

interface IgnoredUserRepository : CrudRepository<IgnoredUserEntity, String>
