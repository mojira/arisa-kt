package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The group or role to which this item is visible.
 *
 * @param identifier The ID of the group or the name of the role that visibility of this item is restricted to.
 * @param type Whether visibility of this item is restricted to a group or role.
 * @param value The name of the group or role that visibility of this item is restricted to. Please note that the name
 *  of a group is mutable, to reliably identify a group use `identifier`.
 */
@Serializable
data class Visibility(
    // The ID of the group or the name of the role that visibility of this item is restricted to.
    @SerialName("identifier")
    val identifier: String? = null,
    // Whether visibility of this item is restricted to a group or role.
    @SerialName("type")
    val type: String? = null,
    // The name of the group or role that visibility of this item is restricted to. Please note that the name of
    // a group is mutable, to reliably identify a group use `identifier`.
    @SerialName("value")
    val value: String? = null
) {
    /**
     * Whether visibility of this item is restricted to a group or role.
     *
     * Values: group, role
     */
    @Serializable
    enum class Type(
        val value: String
    ) {
        @SerialName("group")
        Group("group"),

        @SerialName("role")
        Role("role")
    }
}
