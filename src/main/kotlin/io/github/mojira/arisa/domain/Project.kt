package io.github.mojira.arisa.domain

data class Project(
    val key: String,
    val versions: List<Version>,
    val privateSecurity: String
)