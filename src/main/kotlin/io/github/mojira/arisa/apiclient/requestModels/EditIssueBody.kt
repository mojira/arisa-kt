package io.github.mojira.arisa.apiclient.requestModels

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class EditIssueBody(
    val fields: JsonElement? = null,
    val update: JsonElement? = null
)
