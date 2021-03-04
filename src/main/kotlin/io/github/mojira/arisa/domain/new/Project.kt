package io.github.mojira.arisa.domain.new

import io.github.mojira.arisa.domain.new.Version

data class Project(
    val key: String,
    val versions: List<Version>,
    val privateSecurity: String
)