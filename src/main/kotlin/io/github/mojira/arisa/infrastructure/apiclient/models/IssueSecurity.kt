package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.Serializable

@Serializable
data class IssueSecurity(
    val id: String,
    val self: String,
    val description: String,
    val name: String,
)
