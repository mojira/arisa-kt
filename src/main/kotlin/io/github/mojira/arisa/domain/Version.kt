package io.github.mojira.arisa.domain

import arrow.core.Either

data class Version(
    val id: String,
    val released: Boolean,
    val archived: Boolean,
    val add: () -> Either<Throwable, Unit>,
    val remove: () -> Either<Throwable, Unit>
)
