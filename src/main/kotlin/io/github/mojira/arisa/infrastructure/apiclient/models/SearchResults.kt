package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResults(
    @SerialName("issues")
    val issues: List<IssueBean>,
    val maxResults: Int,
    val startAt: Int,
    val total: Int
)
