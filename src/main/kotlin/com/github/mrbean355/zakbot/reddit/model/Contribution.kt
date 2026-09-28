package com.github.mrbean355.zakbot.reddit.model

import java.time.Instant

sealed interface Contribution {
    val id: String
    val fullName: String
    val author: String
    val created: Instant
    val url: String
}

data class Submission(
    override val id: String,
    override val fullName: String,
    override val author: String,
    override val created: Instant,
    override val url: String,
    val title: String,
    val selfText: String?,
) : Contribution

data class Comment(
    override val id: String,
    override val fullName: String,
    override val author: String,
    override val created: Instant,
    override val url: String,
    val body: String,
    val submissionFullName: String,
    val parentFullName: String,
) : Contribution
