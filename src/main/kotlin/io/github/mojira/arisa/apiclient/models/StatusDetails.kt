package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A status.
 *
 * @param description The description of the status.
 * @param iconUrl The URL of the icon used to represent the status.
 * @param id The ID of the status.
 * @param name The name of the status.
 * @param scope The scope of the field.
 * @param self The URL of the status.
 * @param statusCategory The category assigned to the status.
 */
@Serializable
data class StatusDetails(
    // The description of the status.
    @SerialName("description")
    val description: String? = null,
    // The URL of the icon used to represent the status.
    @SerialName("iconUrl")
    val iconUrl: String? = null,
    // The ID of the status.
    @SerialName("id")
    val id: String? = null,
    // The name of the status.
    @SerialName("name")
    val name: String? = null,
    // The scope of the field.
    @SerialName("scope")
    val scope: Scope? = null,
    // The URL of the status.
    @SerialName("self")
    val self: String? = null,
    // The category assigned to the status.
    @SerialName("statusCategory")
    val statusCategory: StatusCategory? = null
)
