package io.github.mojira.arisa.domain

import arrow.core.Either

data class LinkedIssue<FIELD, FUNPARAM>(
    val key: String,
    val status: String,
    val setField: (FUNPARAM) -> Either<Throwable, Unit>,
    val getField: () -> Either<Throwable, FIELD>
)
