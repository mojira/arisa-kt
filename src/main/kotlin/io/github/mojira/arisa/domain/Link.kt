package io.github.mojira.arisa.domain

import arrow.core.Either
import arrow.core.left

data class Link<FIELD, FUNPARAM>(
    val type: String,
    val outwards: Boolean,
    val issue: LinkedIssue<FIELD, FUNPARAM>,
    val remove: () -> Either<Throwable, Unit> = { UnsupportedOperationException().left() }
)
