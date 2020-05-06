package io.github.mojira.arisa.domain

import arrow.core.Either
import java.util.Date

data class Attachment(
    val name: String,
    val created: Date,
    val remove: () -> Either<Throwable, Unit>,
    val getContent: () -> ByteArray
)
