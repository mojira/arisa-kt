package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class IssueType(
    val self: String? = null,
    val id: String? = null,
    val description: String? = null,
    val iconUrl: String? = null,
    val name: String? = null,
    val subtask: Boolean = false,
    val fields: JsonObject? = null
)
