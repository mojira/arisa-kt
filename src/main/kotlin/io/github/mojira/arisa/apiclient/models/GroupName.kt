package io.github.mojira.arisa.apiclient.models

import URISerializer
import kotlinx.serialization.Serializable

/**
 * Details about a group.
 *
 * @param groupId The ID of the group, which uniquely identifies the group across all Atlassian products. For example, *952d12c3-5b5b-4d04-bb32-44d383afc4b2*.
 * @param name The name of group.
 * @param self The URL for these group details.
 */
@Serializable
data class GroupName(
    val groupId: String,
    val name: String,
    @Serializable(with = URISerializer::class)
    val self: java.net.URI? = null
)
