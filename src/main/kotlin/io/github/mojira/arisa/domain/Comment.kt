package io.github.mojira.arisa.domain

import java.time.Instant

data class Comment(
    val id: String,
    val body: String,
    val author: User,
    val created: Instant,
    val updated: Instant,
    val visibilityType: String?,
    val visibilityValue: String?,
)