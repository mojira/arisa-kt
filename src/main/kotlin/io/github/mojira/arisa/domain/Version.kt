package io.github.mojira.arisa.domain

import java.time.Instant

data class Version(
    val id: String,
    val released: Boolean,
    val archived: Boolean,
    val releaseDate: Instant?,
    val add: () -> Unit,
    val remove: () -> Unit
)
