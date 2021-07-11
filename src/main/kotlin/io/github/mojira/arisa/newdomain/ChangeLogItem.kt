package io.github.mojira.arisa.newdomain

import java.time.Instant

data class ChangeLogItem(
    val created: Instant,
    val field: String,
    val changedFrom: String?,
    val changedFromString: String?,
    var changedTo: String?,
    var changedToString: String?,
    val author: User
)
