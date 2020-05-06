package io.github.mojira.arisa.domain

import arrow.core.Either
import java.time.Instant

data class Attachment(
    val name: String,
    val created: Instant,
    val remove: () -> Either<Throwable, Unit>,
    val getContent: () -> ByteArray
)
