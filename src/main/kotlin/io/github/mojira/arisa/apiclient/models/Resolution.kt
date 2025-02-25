package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.Serializable

@Serializable
data class Resolution(
    val self: String? = null,
    val id: String? = null,
    val description: String? = null,
    val name: String? = null,
    val iconUrl: String? = null,
    val default: Boolean? = null
)
