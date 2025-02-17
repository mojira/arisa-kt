package io.github.mojira.arisa.domain.cloud

import arrow.core.Either

data class CloudLinkedIssue(
    val key: String,
    val status: String,
    val getFullIssue: () -> Either<Throwable, CloudIssue>,
    val createLink: (type: String, key: String, outwards: Boolean) -> Unit
)
