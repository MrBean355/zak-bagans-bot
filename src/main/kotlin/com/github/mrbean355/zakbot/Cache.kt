package com.github.mrbean355.zakbot

import com.github.mrbean355.zakbot.util.SystemClock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Component
import java.io.File

private const val FILE_NAME = "cache.json"

@Component
class Cache(systemClock: SystemClock) {
    private val data: Data

    init {
        val file = File(FILE_NAME)
        data = if (file.exists()) {
            Json.decodeFromString(file.readText())
        } else {
            Data(
                lastPost = systemClock.currentTimeMillis,
                lastComment = systemClock.currentTimeMillis
            )
        }
        save()
    }

    fun getLastPost(): Long = data.lastPost

    fun setLastPost(time: Long) {
        data.lastPost = time
        save()
    }

    fun getLastComment(): Long = data.lastComment

    fun setLastComment(time: Long) {
        data.lastComment = time
        save()
    }

    private fun save() = synchronized(this) {
        File(FILE_NAME).writeText(Json.encodeToString(data))
    }

    @Serializable
    private data class Data(
        var lastPost: Long,
        var lastComment: Long
    )
}