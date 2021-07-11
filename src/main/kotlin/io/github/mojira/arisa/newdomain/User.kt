package io.github.mojira.arisa.newdomain

data class User(
    val name: String?,
    val displayName: String?,
    val groups: List<String>,
    val isNewUser: Boolean
)
