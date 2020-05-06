package io.github.mojira.arisa.domain

import arrow.core.Either

data class Link<FIELD, FUNPARAM>(
    val type: String,
    val outwards: Boolean,
    val issue: LinkedIssue<FIELD, FUNPARAM>,
    val remove: () -> Either<Throwable, Unit>
)
