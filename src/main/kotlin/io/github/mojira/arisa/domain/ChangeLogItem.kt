package io.github.mojira.arisa.domain

data class ChangeLogItem(
    val created: Long,
    val field: String,
    val changedTo: String?,
    val changedFrom: String?,
    val getAuthorGroups: () -> List<String>?
)
