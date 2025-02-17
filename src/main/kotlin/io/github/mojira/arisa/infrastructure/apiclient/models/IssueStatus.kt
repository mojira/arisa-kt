package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IssueStatus(
    @SerialName("self")
    val self: String? = null,
    @SerialName("id")
    val id: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("iconUrl")
    val iconUrl: String? = null,
    @SerialName("name")
    val name: String
)