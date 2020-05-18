package io.github.mojira.arisa.domain

import arrow.core.Either
import java.time.Instant

data class Comment(
    val body: String,
    val author: User,
    val getAuthorGroups: () -> List<String>?,
    val created: Instant,
    val updated: Instant,
    val visibilityType: String?,
    val visibilityValue: String?,
    val restrict: (String) -> Either<Throwable, Unit>,
    val update: (String) -> Either<Throwable, Unit>
)
