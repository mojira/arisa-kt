package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchResults(
    val issues: List<IssueBean>,
    val maxResults: Int,
    val startAt: Int,
    val total: Int
)
