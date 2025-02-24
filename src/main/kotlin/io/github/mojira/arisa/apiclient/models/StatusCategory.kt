package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.SerialName
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
    /* The name of the color used to represent the status category. */
    @SerialName("colorName")
    val colorName: String? = null,

    /* The ID of the status category. */
    @SerialName("id")
    val id: Long? = null,

    /* The key of the status category. */
    @SerialName("key")
    val key: String? = null,

    /* The name of the status category. */
    @SerialName("name")
    val name: String? = null,

    /* The URL of the status category. */
    @SerialName("self")
    val self: String? = null
)
