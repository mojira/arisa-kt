package io.github.mojira.arisa.domain

import java.time.Instant

data class ChangeLogItem(
    /** ID of the enclosing change log entry */
    val entryId: String,
    /** 0-based index of this change log item within the enclosing change log entry */
    val itemIndex: Int,
    val created: Instant,
    val field: String,
    val changedFrom: String?,
    val changedFromString: String?,
    val changedTo: String?,
    val changedToString: String?,
    val author: User,
    val getAuthorGroups: () -> List<String>?
)
