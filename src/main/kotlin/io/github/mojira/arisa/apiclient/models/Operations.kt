package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.Serializable

/**
 * Details of the operations that can be performed on the issue.
 *
 * @param linkGroups Details of the link groups defining issue operations.
 */
@Serializable
data class Operations(
    val linkGroups: List<LinkGroup>? = null
)
