package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.Serializable

@Serializable
data class IssueStatus(
    val self: String? = null,
    val id: String? = null,
    val description: String? = null,
    val iconUrl: String? = null,
    val name: String
)
