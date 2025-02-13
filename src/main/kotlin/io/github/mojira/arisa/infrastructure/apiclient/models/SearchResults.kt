package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys
data class SearchResults(
    @SerialName("issues")
    val issues: List<IssueBean>
) {}
