package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.Serializable

@Serializable
data class Attachment(
    val id: String,
    val filename: String,
    val content: String,
    val mimeType: String,
    val size: Long,
    val created: String,
    val author: User
)
