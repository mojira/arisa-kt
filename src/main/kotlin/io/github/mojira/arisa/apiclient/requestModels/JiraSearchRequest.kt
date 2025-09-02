package io.github.mojira.arisa.apiclient.requestModels

import kotlinx.serialization.Serializable

@Serializable
data class JiraSearchRequest(
    val expand: String,
    val fields: List<String>,
    val fieldsByKeys: Boolean? = null,
    val jql: String,
    val maxResults: Int,
    val nextPageToken: String? = null
)
