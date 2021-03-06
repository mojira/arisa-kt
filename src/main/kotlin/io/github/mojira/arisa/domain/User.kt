package io.github.mojira.arisa.domain

data class User(
    val name: String?,
    val displayName: String?,
    val groups: List<String>
)