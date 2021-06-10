package io.github.mojira.arisa.domain

import java.time.Instant

data class Version(
    val id: String,
    val name: String,
    val released: Boolean,
    val archived: Boolean,
    val releaseDate: Instant?
)
