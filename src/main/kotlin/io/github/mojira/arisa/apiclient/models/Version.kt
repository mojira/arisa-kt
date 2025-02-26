package io.github.mojira.arisa.apiclient.models

import io.github.mojira.arisa.apiclient.serializers.DateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class Version(
    val id: String,
    val name: String,
    @SerialName("archived")
    val isArchived: Boolean,
    @SerialName("released")
    val isReleased: Boolean,
    @Serializable(with = DateSerializer::class)
    val startDate: LocalDate? = null,
    @Serializable(with = DateSerializer::class)
    val releaseDate: LocalDate? = null
)
