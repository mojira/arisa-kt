package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.Serializable

@Serializable
data class IssuePriority(
    val self: String? = null,
    val id: String? = null,
    val iconUrl: String? = null,
    val name: String? = null
)
