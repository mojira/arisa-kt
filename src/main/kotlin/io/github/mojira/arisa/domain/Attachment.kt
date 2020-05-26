package io.github.mojira.arisa.domain

import java.time.Instant

data class Attachment(
    val name: String,
    val created: Instant,
    val remove: () -> Unit,
    val getContent: () -> ByteArray
)
