package io.github.mojira.arisa.apiclient.requestModels

import io.github.mojira.arisa.apiclient.models.EntityProperty
import io.github.mojira.arisa.apiclient.models.HistoryMetadata
import io.github.mojira.arisa.apiclient.models.IssueTransition
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TransitionIssueBody(
    val fields: JsonElement? = null,
    val historyMetadata: HistoryMetadata? = null,
    val properties: List<EntityProperty> = emptyList(),
    val transition: IssueTransition? = null,
    val update: JsonElement? = null
) : HashMap<String, JsonElement>()
