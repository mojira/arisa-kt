package io.github.mojira.arisa.apiclient.models

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
    val defaultGroups: Set<String>? = null,
    val defaultGroupsDetails: List<GroupName>? = null,
    val defined: Boolean? = null,
    val groupDetails: List<GroupName>? = null,
    val groups: Set<String>? = null,
    val hasUnlimitedSeats: Boolean? = null,
    val key: String? = null,
    val name: String? = null,
    val numberOfSeats: Int? = null,
    val platform: Boolean? = null,
    val remainingSeats: Int? = null,
    val selectedByDefault: Boolean? = null,
    val userCount: Int? = null,
    val userCountDescription: String? = null
)
