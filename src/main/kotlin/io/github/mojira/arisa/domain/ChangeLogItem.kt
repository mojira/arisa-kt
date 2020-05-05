package io.github.mojira.arisa.domain

import java.time.Instant

data class ChangeLogItem(
    val created: Instant,
    val field: String,
    val changedTo: String?,
    val changedFrom: String?,
    val getAuthorGroups: () -> List<String>?
)
