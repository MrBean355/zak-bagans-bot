package com.github.mrbean355.zakbot.db.repo

import com.github.mrbean355.zakbot.db.entity.IgnoredSubmissionEntity
import org.springframework.data.repository.CrudRepository

interface IgnoredSubmissionRepository : CrudRepository<IgnoredSubmissionEntity, String>
