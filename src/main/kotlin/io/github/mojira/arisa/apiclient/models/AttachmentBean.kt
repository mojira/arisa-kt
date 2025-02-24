package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AttachmentBean(
    @SerialName("id")
    val id: String,

    @SerialName("filename")
    val filename: String,

    @SerialName("created")
    val created: String,

    @SerialName("mimeType")
    val mimeType: String,

    @SerialName("content")
    val content: String,

    @SerialName("author")
    val author: UserDetails? = null,

    @SerialName("size")
    val size: Int,
)
