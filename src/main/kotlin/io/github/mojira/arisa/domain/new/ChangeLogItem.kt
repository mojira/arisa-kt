package io.github.mojira.arisa.domain.new

import java.time.Instant

data class ChangeLogItem(
    val created: Instant,
    val field: String,
    val changedFrom: String?,
    val changedFromString: String?,
    val changedTo: String?,
    val changedToString: String?,
    val author: User
)