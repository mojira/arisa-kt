package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.Serializable

@Serializable
data class AttachmentBean(
    val id: String,
    val filename: String,
    val created: String,
    val mimeType: String? = null,
    val content: String,
    val author: UserDetails? = null,
    val size: Int
)
