package io.github.mojira.arisa.domain

import java.time.Instant

data class Attachment(
    val id: String,
    val name: String,
    val created: Instant,
    val mimeType: String,
    val remove: () -> Unit,
    val getContent: () -> ByteArray,
    val uploader: User
)
