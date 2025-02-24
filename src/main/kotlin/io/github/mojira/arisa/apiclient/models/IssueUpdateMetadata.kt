package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.Serializable

/**
 * A list of editable field details.
 *
 * @param fields
 */
@Serializable
data class IssueUpdateMetadata(
    val fields: Map<String, FieldMetadata>? = null
)
