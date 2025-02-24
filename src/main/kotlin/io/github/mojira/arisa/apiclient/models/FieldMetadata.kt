package io.github.mojira.arisa.apiclient.models

import io.github.mojira.arisa.apiclient.OpenApiObject
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
    val key: String,
    val name: String,
    val operations: List<String>,
    val required: Boolean,
    val schema: JsonTypeBean,
    //    val allowedValues: List<Any>? = null,
    val autoCompleteUrl: String? = null,
    val configuration: OpenApiObject? = null,
    //    val defaultValue: Any? = null,
    val hasDefaultValue: Boolean? = null
)
