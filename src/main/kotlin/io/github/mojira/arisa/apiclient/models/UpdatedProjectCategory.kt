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
    /* The name of the project category. */
    @SerialName("description")
    val description: String? = null,

    /* The ID of the project category. */
    @SerialName("id")
    val id: String? = null,

    /* The description of the project category. */
    @SerialName("name")
    val name: String? = null,

    /* The URL of the project category. */
    @SerialName("self")
    val self: String? = null
)
