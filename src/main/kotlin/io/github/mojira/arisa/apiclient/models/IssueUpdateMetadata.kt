package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A list of editable field details.
 *
 * @param fields
 */
@Serializable
data class IssueUpdateMetadata(
    @SerialName("fields")
    val fields: Map<String, FieldMetadata>? = null
)
