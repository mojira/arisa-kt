package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.Serializable

/**
 * A status category.
 *
 * @param colorName The name of the color used to represent the status category.
 * @param id The ID of the status category.
 * @param key The key of the status category.
 * @param name The name of the status category.
 * @param self The URL of the status category.
 */
@Serializable
data class StatusCategory(
    val colorName: String? = null,
    val id: Long? = null,
    val key: String? = null,
    val name: String? = null,
    val self: String? = null
)
