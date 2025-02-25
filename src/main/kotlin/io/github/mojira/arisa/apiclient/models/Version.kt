package io.github.mojira.arisa.apiclient.models

import io.github.mojira.arisa.apiclient.serializers.OffsetDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class Version(
    val id: String,
    val name: String,
    @SerialName("archived")
    val isArchived: Boolean,
    @SerialName("released")
    val isReleased: Boolean,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val releaseDate: OffsetDateTime? = null
)
