package io.github.mojira.arisa.infrastructure.apiclient.requestModels

import io.github.mojira.arisa.infrastructure.apiclient.OpenApiObject
import io.github.mojira.arisa.infrastructure.apiclient.models.EntityProperty
import io.github.mojira.arisa.infrastructure.apiclient.models.HistoryMetadata
import io.github.mojira.arisa.infrastructure.apiclient.models.IssueTransition
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TransitionIssueBody(
    val fields: OpenApiObject,
    val historyMetadata: HistoryMetadata,
    val properties: List<EntityProperty>,
    val transition: IssueTransition,
    val update: OpenApiObject,
): HashMap<String, JsonElement>()
