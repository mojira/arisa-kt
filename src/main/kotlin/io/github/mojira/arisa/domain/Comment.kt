package io.github.mojira.arisa.domain

import java.time.Instant

data class Comment(
    val id: String?,
    var body: String, // TODO: Map to Jira
    val author: User?,
    val created: Instant,
    val updated: Instant?,
    val visibilityType: String?,
    val visibilityValue: String?,
)