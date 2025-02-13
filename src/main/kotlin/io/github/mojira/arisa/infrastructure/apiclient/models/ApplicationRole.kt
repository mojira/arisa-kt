package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Details of an application role.
 *
 * @param defaultGroups The groups that are granted default access for this application role. As a group's name can
 * change, use of `defaultGroupsDetails` is recommended to identify a groups.
 * @param defaultGroupsDetails The groups that are granted default access for this application role.
 * @param defined Deprecated.
 * @param groupDetails The groups associated with the application role.
 * @param groups The groups associated with the application role. As a group's name can change, use of `groupDetails` is
 * recommended to identify a groups.
 * @param hasUnlimitedSeats Whether the role has unlimited seats
 * @param key The key of the application role.
 * @param name The display name of the application role.
 * @param numberOfSeats The maximum count of users on your license.
 * @param platform Indicates if the application role belongs to Jira platform (`jira-core`).
 * @param remainingSeats The count of users remaining on your license.
 * @param selectedByDefault Determines whether this application role should be selected by default on user creation.
 * @param userCount The number of users counting against your license.
 * @param userCountDescription The type of users being counted against your license.
 */
@Serializable
data class ApplicationRole(
    // The groups that are granted default access for this application role.
    @SerialName("defaultGroups")
    val defaultGroups: Set<String>? = null,
    // The groups that are granted default access for this application role.
    @SerialName("defaultGroupsDetails")
    val defaultGroupsDetails: List<GroupName>? = null,
    // Deprecated.
    @SerialName("defined")
    val defined: Boolean? = null,
    // The groups associated with the application role.
    @SerialName("groupDetails")
    val groupDetails: List<GroupName>? = null,
    // The groups associated with the application role.
    @SerialName("groups")
    val groups: Set<String>? = null,
    @SerialName("hasUnlimitedSeats")
    val hasUnlimitedSeats: Boolean? = null,
    // The key of the application role.
    @SerialName("key")
    val key: String? = null,
    // The display name of the application role.
    @SerialName("name")
    val name: String? = null,
    // The maximum count of users on your license.
    @SerialName("numberOfSeats")
    val numberOfSeats: Int? = null,
    // Indicates if the application role belongs to Jira platform (`jira-core`).
    @SerialName("platform")
    val platform: Boolean? = null,
    // The count of users remaining on your license.
    @SerialName("remainingSeats")
    val remainingSeats: Int? = null,
    // Determines whether this application role should be selected by default on user creation.
    @SerialName("selectedByDefault")
    val selectedByDefault: Boolean? = null,
    // The number of users counting against your license.
    @SerialName("userCount")
    val userCount: Int? = null,
    // The type of users being counted against your license.
    @SerialName("userCountDescription")
    val userCountDescription: String? = null
)
