package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.Serializable

/**
 * @param actuallyIncluded
 * @param excluded
 * @param included
 */
@Serializable
data class IncludedFields(
    val actuallyIncluded: Set<String>? = null,
    val excluded: Set<String>? = null,
    val included: Set<String>? = null
)
