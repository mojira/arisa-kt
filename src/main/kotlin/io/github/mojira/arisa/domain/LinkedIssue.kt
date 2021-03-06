package io.github.mojira.arisa.domain

import arrow.core.Either

data class LinkedIssue(
    val key: String,
    val status: String,
    val getFullIssue: () -> Either<Throwable, Issue>,
    val createLink: (type: String, key: String, outwards: Boolean) -> Unit
)
