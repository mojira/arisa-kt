package io.github.mojira.arisa.apiclient.models

import io.github.mojira.arisa.apiclient.OpenApiObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The schema of a field.
 *
 * @param type The data type of the field.
 * @param configuration If the field is a custom field, the configuration of the field.
 * @param custom If the field is a custom field, the URI of the field.
 * @param customId If the field is a custom field, the custom ID of the field.
 * @param items When the data type is an array, the name of the field items within the array.
 * @param system If the field is a system field, the name of the field.
 */
@Serializable
data class JsonTypeBean(
    @SerialName("type")
    val type: String,
    val configuration: OpenApiObject? = null,
    val custom: String? = null,
    val customId: Long? = null,
    val items: String? = null,
    val system: String? = null
)
