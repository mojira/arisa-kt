package io.github.mojira.arisa.infrastructure.apiclient.requestModels

import io.github.mojira.arisa.infrastructure.apiclient.OpenApiObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class EditIssueBody(
    val fields: JsonElement? = null,
    val update: JsonElement? = null,
)
