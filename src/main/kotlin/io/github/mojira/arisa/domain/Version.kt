package io.github.mojira.arisa.domain

import arrow.core.Either

data class Version(
    val released: Boolean,
    val archived: Boolean,
    val execute: () -> Either<Throwable, Unit>
)
