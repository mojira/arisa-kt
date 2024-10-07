package io.github.mojira.arisa.domain

import java.time.Instant

data class Comment(
    val id: String,
    val body: String?,
    val author: User?,
    val getAuthorGroups: () -> List<String>?,
    val created: Instant,
    val updated: Instant,
    val visibilityType: String?,
    val visibilityValue: String?,
    val restrict: (String) -> Unit,
    val update: (String) -> Unit,
    val remove: () -> Unit
)
