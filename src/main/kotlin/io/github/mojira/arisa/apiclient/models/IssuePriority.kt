package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IssuePriority(
    @SerialName("self")
    val self: String? = null,
    @SerialName("id")
    val id: String? = null,
    @SerialName("iconUrl")
    val iconUrl: String? = null,
    @SerialName("name")
    val name: String? = null
) 