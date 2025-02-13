package io.github.mojira.arisa.infrastructure.apiclient.models

import io.github.mojira.arisa.infrastructure.apiclient.OpenApiObject
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

    /* The data type of the field. */
    @SerialName("type")
    val type: String,

    /* If the field is a custom field, the configuration of the field. */
    @SerialName("configuration")
    val configuration: OpenApiObject? = null,

    /* If the field is a custom field, the URI of the field. */
    @SerialName("custom")
    val custom: String? = null,

    /* If the field is a custom field, the custom ID of the field. */
    @SerialName("customId")
    val customId: Long? = null,

    /* When the data type is an array, the name of the field items within the array. */
    @SerialName("items")
    val items: String? = null,

    /* If the field is a system field, the name of the field. */
    @SerialName("system")
    val system: String? = null

)
