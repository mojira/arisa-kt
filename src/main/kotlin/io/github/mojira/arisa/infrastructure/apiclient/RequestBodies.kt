package io.github.mojira.arisa.infrastructure.apiclient

import kotlinx.serialization.Serializable

@Serializable
data class JiraSearchRequest(
    val expand: List<String>,
    val fields: List<String>,
    val fieldsByKeys: Boolean? = null,
    val jql: String,
    val maxResults: Int,
    val startAt: Int
)