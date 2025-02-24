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
    val description: String? = null,
    val iconUrl: String? = null,
    val id: String? = null,
    val name: String? = null,
    val scope: Scope? = null,
    val self: String? = null,
    val statusCategory: StatusCategory? = null
)
