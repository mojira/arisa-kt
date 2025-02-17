package io.github.mojira.arisa.infrastructure.apiclient.requestModels

import io.github.mojira.arisa.infrastructure.apiclient.OpenApiObject
import kotlinx.serialization.Serializable

@Serializable
data class EditIssueBody(
    val fields: OpenApiObject? = null,
    val update: OpenApiObject? = null,
)
