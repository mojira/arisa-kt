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

    /* The name of the field changed. */
    @SerialName("field")
    val `field`: String,

    /* The ID of the field changed. */
    @SerialName("fieldId")
    val fieldId: String? = null,

    /* The type of the field changed. */
    @SerialName("fieldtype")
    val fieldtype: String? = null,

    /* The details of the original value. */
    @SerialName("from")
    val from: String? = null,

    /* The details of the original value as a string. */
    @SerialName("fromString")
    val fromString: String? = null,

    /* The details of the new value. */
    @SerialName("to")
    val to: String? = null,

    /* The details of the new value as a string. */
    @SerialName("toString")
    val toString: String? = null

)
