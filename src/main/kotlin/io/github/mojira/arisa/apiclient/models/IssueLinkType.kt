package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.Serializable

@Serializable
data class IssueLinkType(
    val id: String? = null,
    val inward: String? = null,
    val name: String? = null,
    val outward: String? = null,
    val self: String? = null
)
