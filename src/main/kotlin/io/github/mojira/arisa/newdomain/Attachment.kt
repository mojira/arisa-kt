package io.github.mojira.arisa.newdomain

import java.io.InputStream
import java.time.Instant

data class Attachment(
    val id: String,
    val name: String,
    val created: Instant,
    val mimeType: String,
    val openContentStream: () -> InputStream,
    val getContent: () -> ByteArray,
    val uploader: User?,
    var exists: Boolean = true
)
