package com.github.mrbean355.zakbot.service

import com.github.mrbean355.zakbot.db.entity.IgnoredSubmissionEntity
import com.github.mrbean355.zakbot.db.entity.IgnoredUserEntity
import com.github.mrbean355.zakbot.db.entity.LastCheckedEntity
import com.github.mrbean355.zakbot.db.repo.IgnoredSubmissionRepository
import com.github.mrbean355.zakbot.db.repo.IgnoredUserRepository
import com.github.mrbean355.zakbot.db.repo.LastCheckedRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

private const val SubmissionKey = "post"
private const val CommentKey = "comment"

@Service
@Transactional
class BotStateService(
    private val lastCheckedRepository: LastCheckedRepository,
    private val ignoredUserRepository: IgnoredUserRepository,
    private val ignoredSubmissionRepository: IgnoredSubmissionRepository,
    private val clock: Clock,
) {

    fun getLastSubmissionTime(): Instant {
        return lastCheckedRepository.findByIdOrNull(SubmissionKey)?.value
            ?: lastCheckedRepository.save(LastCheckedEntity(SubmissionKey, currentTime())).value
    }

    fun setLastSubmissionTime(time: Instant) {
        lastCheckedRepository.save(LastCheckedEntity(SubmissionKey, time))
    }

    fun getLastCommentTime(): Instant {
        return lastCheckedRepository.findByIdOrNull(CommentKey)?.value
            ?: lastCheckedRepository.save(LastCheckedEntity(CommentKey, currentTime())).value
    }

    fun setLastCommentTime(time: Instant) {
        lastCheckedRepository.save(LastCheckedEntity(CommentKey, time))
    }

    @Transactional(readOnly = true)
    fun isUserIgnored(id: String): Boolean {
        return ignoredUserRepository.existsById(id)
    }

    fun ignoreUser(id: String, source: String) {
        ignoredUserRepository.save(IgnoredUserEntity(id, currentTime(), source))
    }

    fun unignoreUser(id: String) {
        ignoredUserRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun isSubmissionIgnored(fullName: String): Boolean {
        return ignoredSubmissionRepository.existsById(fullName)
    }

    fun ignoreSubmission(fullName: String, reason: String?) {
        ignoredSubmissionRepository.save(IgnoredSubmissionEntity(fullName, currentTime(), reason))
    }

    fun unignoreSubmission(fullName: String) {
        ignoredSubmissionRepository.deleteById(fullName)
    }

    private fun currentTime(): Instant = clock.instant()
}