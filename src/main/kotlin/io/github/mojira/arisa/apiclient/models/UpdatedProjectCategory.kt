package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A project category.
 *
 * @param description The name of the project category.
 * @param id The ID of the project category.
 * @param name The description of the project category.
 * @param self The URL of the project category.
 */
@Serializable
data class UpdatedProjectCategory(
    val description: String? = null,
    val id: String? = null,
    val name: String? = null,
    val self: String? = null
)
