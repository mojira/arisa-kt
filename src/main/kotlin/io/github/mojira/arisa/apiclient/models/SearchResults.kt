package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchResults(
    val isLast: Boolean,
    val issues: List<IssueBean>,
    val nextPageToken: String? = null
)
