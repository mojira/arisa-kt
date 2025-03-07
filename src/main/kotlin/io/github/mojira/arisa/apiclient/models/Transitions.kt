package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.Serializable

/**
 * List of issue transitions.
 *
 * @param expand Expand options that include additional transitions details in the response.
 * @param transitions List of issue transitions.
 */
@Serializable
data class Transitions(
    val expand: String? = null,
    val transitions: List<IssueTransition>? = null
)
