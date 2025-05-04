package io.github.mojira.arisa.domain

data class User(
    val accountId: String,
    @Deprecated("Use `accountId` to identify users.")
    val name: String?,
    val displayName: String?,
    val getGroups: () -> List<String>?,
    val isBotUser: () -> Boolean
)
