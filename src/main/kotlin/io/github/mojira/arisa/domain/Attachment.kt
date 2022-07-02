package io.github.mojira.arisa.domain

import java.io.InputStream
import java.time.Instant

data class Attachment(
    val id: String,
    val name: String,
    val created: Instant,
    val mimeType: String,
    val remove: () -> Unit,
    val openContentStream: () -> InputStream,
    val getContent: () -> ByteArray,
    val uploader: User?
) {
    /** Returns whether the type of the content is text */
    fun hasTextContent() = mimeType.startsWith("text/") or
            (mimeType == "application/json") or (mimeType == "application/xml")

    /** Decodes the content as UTF-8 String */
    fun getTextContent() = String(getContent())
}
