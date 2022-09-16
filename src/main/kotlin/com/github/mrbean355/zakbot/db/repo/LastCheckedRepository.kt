package com.github.mrbean355.zakbot.db.repo

import com.github.mrbean355.zakbot.db.entity.LastCheckedEntity
import org.springframework.data.repository.CrudRepository

interface LastCheckedRepository : CrudRepository<LastCheckedEntity, String>
