package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.SerialName
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

    @SerialName("groups")
    val groups: kotlin.collections.List<LinkGroup>? = null,

    @SerialName("header")
    val header: SimpleLink? = null,

    @SerialName("id")
    val id: kotlin.String? = null,

    @SerialName("links")
    val links: kotlin.collections.List<SimpleLink>? = null,

    @SerialName("styleClass")
    val styleClass: kotlin.String? = null,

    @SerialName("weight")
    val weight: kotlin.Int? = null

)
