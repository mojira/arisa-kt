package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class IssueType(
    @SerialName("self")
    val self: String? = null,
    @SerialName("id")
    val id: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("iconUrl")
    val iconUrl: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("subtask")
    val subtask: Boolean = false,
    @SerialName("fields")
    val fields: JsonObject? = null
) 