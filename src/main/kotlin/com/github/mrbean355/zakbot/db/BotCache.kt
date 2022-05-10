package com.github.mrbean355.zakbot.db

import com.github.mrbean355.zakbot.util.SystemClock
import org.springframework.stereotype.Component
import java.util.Date
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val PostKey = "post"
private const val CommentKey = "comment"

@Component
class BotCache(
    private val lastCheckedRepository: LastCheckedRepository,
    private val ignoredUserRepository: IgnoredUserRepository,
    private val systemClock: SystemClock
) {
    private val lock = ReentrantLock()

    fun getLastPostTime(): Date = lock.withLock {
        val entity = lastCheckedRepository.findById(PostKey)
        if (entity.isPresent) {
            entity.get().value
        } else {
            lastCheckedRepository.save(LastChecked(PostKey, currentTime())).value
        }
    }

    fun setLastPostTime(time: Date) {
        lock.withLock {
            lastCheckedRepository.save(LastChecked(PostKey, time))
        }
    }

    fun getLastCommentTime(): Date = lock.withLock {
        val entity = lastCheckedRepository.findById(CommentKey)
        if (entity.isPresent) {
            entity.get().value
        } else {
            lastCheckedRepository.save(LastChecked(CommentKey, currentTime())).value
        }
    }

    fun setLastCommentTime(time: Date) {
        lock.withLock {
            lastCheckedRepository.save(LastChecked(CommentKey, time))
        }
    }

    fun isUserIgnored(id: String): Boolean {
        return ignoredUserRepository.findById(id).isPresent
    }

    fun ignoreUser(id: String, source: String) {
        ignoredUserRepository.save(IgnoredUser(id, currentTime(), source))
    }

    private fun currentTime() = Date(systemClock.currentTimeMillis)

}