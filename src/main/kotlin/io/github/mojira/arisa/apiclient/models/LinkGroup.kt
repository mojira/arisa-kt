package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.Serializable

/**
 * Details a link group, which defines issue operations.
 *
 * @param groups
 * @param header
 * @param id
 * @param links
 * @param styleClass
 * @param weight
 */
@Serializable
data class LinkGroup(
    val groups: List<LinkGroup>? = null,
    val header: SimpleLink? = null,
    val id: String? = null,
    val links: List<SimpleLink>? = null,
    val styleClass: String? = null,
    val weight: Int? = null
)
