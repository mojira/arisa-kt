package io.github.mojira.arisa.infrastructure.apiclient.models

import io.github.mojira.arisa.infrastructure.apiclient.OpenApiObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The metadata describing an issue field.
 *
 * @param key The key of the field.
 * @param name The name of the field.
 * @param operations The list of operations that can be performed on the field.
 * @param required Whether the field is required.
 * @param schema The data type of the field.
 * @param allowedValues The list of values allowed in the field.
 * @param autoCompleteUrl The URL that can be used to automatically complete the field.
 * @param configuration The configuration properties.
 * @param defaultValue The default value of the field.
 * @param hasDefaultValue Whether the field has a default value.
 */
@Serializable
data class FieldMetadata(

    /* The key of the field. */
    @SerialName("key")
    val key: String,

    /* The name of the field. */
    @SerialName("name")
    val name: String,

    /* The list of operations that can be performed on the field. */
    @SerialName("operations")
    val operations: List<String>,

    /* Whether the field is required. */
    @SerialName("required")
    val required: Boolean,

    /* The data type of the field. */
    @SerialName("schema")
    val schema: JsonTypeBean,

    /* The list of values allowed in the field. */
//    @SerialName("allowedValues")
//    val allowedValues: List<Any>? = null,

    /* The URL that can be used to automatically complete the field. */
    @SerialName("autoCompleteUrl")
    val autoCompleteUrl: String? = null,

    /* The configuration properties. */
    @SerialName("configuration")
    val configuration: OpenApiObject? = null,

    /* The default value of the field. */
//    @SerialName("defaultValue")
//    val defaultValue: Any? = null,

    /* Whether the field has a default value. */
    @SerialName("hasDefaultValue")
    val hasDefaultValue: Boolean? = null

)
