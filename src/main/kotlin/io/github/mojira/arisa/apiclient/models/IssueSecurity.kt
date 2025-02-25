package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.Serializable

@Serializable
data class IssueSecurity(
    val id: String,
    val self: String,
    val description: String,
    val name: String
)
