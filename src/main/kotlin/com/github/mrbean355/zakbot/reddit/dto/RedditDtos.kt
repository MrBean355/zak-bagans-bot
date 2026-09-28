package com.github.mrbean355.zakbot.reddit.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class RedditTokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("token_type") val tokenType: String,
    @JsonProperty("expires_in") val expiresIn: Long,
    @JsonProperty("scope") val scope: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RedditListing<T>(
    val kind: String?,
    val data: RedditListingData<T>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RedditListingData<T>(
    val after: String?,
    val dist: Int?,
    val children: List<RedditThing<T>> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RedditThing<T>(
    val kind: String?,
    val data: T,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RedditSubmissionDto(
    val id: String,
    val name: String,
    val author: String,
    val title: String,
    @JsonProperty("selftext") val selfText: String?,
    @JsonProperty("created_utc") val createdUtc: Double,
    val permalink: String,
    val url: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RedditCommentDto(
    val id: String,
    val name: String,
    val author: String,
    val body: String,
    @JsonProperty("link_id") val linkId: String,
    @JsonProperty("parent_id") val parentId: String,
    @JsonProperty("created_utc") val createdUtc: Double,
    val permalink: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RedditUserAboutDto(
    val data: RedditUserDataDto?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RedditUserDataDto(
    @JsonProperty("is_suspended") val isSuspended: Boolean? = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RedditFlairDto(
    val id: String,
    val text: String,
)
