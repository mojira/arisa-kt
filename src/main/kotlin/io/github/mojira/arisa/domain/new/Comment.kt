package io.github.mojira.arisa.domain.new

import io.github.mojira.arisa.domain.new.User
import java.time.Instant

data class Comment(
    val id: String,
    val body: String?,
    val author: User,
    val created: Instant,
    val updated: Instant,
    val visibilityType: String?,
    val visibilityValue: String?,
)