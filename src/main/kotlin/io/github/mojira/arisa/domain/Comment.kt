package io.github.mojira.arisa.domain

import arrow.core.Either
import java.time.Instant

data class Comment(
    val body: String,
    val authorDisplayName: String,
    val getAuthorGroups: () -> List<String>?,
    val updated: Instant,
    val created: Instant,
    val visibilityType: String?,
    val visibilityValue: String?,
    val restrict: (String) -> Either<Throwable, Unit>,
    val update: (String) -> Either<Throwable, Unit>
)
