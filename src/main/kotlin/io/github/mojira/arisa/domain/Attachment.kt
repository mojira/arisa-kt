package io.github.mojira.arisa.domain

import java.time.Instant
import java.util.function.Supplier

data class Attachment(
    val id: String,
    var name: String,
    var created: Instant,
    var mimeType: String,
    var uploader: User?,
    var content: Supplier<ByteArray>
)