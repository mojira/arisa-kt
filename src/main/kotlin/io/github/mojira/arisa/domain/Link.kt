package io.github.mojira.arisa.domain

import arrow.core.Either

data class Link(
    val type: String,
    val outwards: Boolean,
    val issue: LinkedIssue,
    val remove: () -> Either<Throwable, Unit>
)
