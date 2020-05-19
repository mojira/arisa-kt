package io.github.mojira.arisa.domain

import arrow.core.Either

data class LinkedIssue(
    val key: String,
    val status: String,
    val getFullIssue: () -> Either<Throwable, Issue>,
    val createLink: (key: String, type: String) -> Either<Throwable, Unit>,
    val addAffectedVersion: (id: String) -> Either<Throwable, Unit>
)
