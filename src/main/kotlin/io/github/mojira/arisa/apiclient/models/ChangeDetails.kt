package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A change item.
 *
 * @param `field` The name of the field changed.
 * @param fieldId The ID of the field changed.
 * @param fieldtype The type of the field changed.
 * @param from The details of the original value.
 * @param fromString The details of the original value as a string.
 * @param to The details of the new value.
 * @param toString The details of the new value as a string.
 */
@Serializable
data class ChangeDetails(
    @SerialName("field")
    val `field`: String,
    val fieldId: String? = null,
    val fieldtype: String? = null,
    val from: String? = null,
    val fromString: String? = null,
    val to: String? = null,
    val toString: String? = null
)
