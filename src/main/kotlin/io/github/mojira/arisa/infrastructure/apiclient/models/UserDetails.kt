package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDetails (

    /* The account ID of the user, which uniquely identifies the user across all Atlassian products. For example, *5b10ac8d82e05b22cc7d4ef5*. */
    @SerialName("accountId")
    val accountId: String? = null,

    /* The type of account represented by this user. This will be one of 'atlassian' (normal users), 'app' (application user) or 'customer' (Jira Service Desk customer user) */
    @SerialName("accountType")
    val accountType: String? = null,

    /* Whether the user is active. */
    @SerialName("active")
    val active: Boolean? = null,

    /* The avatars of the user. */
    @SerialName("avatarUrls")
    val avatarUrls: AvatarUrlsBean? = null,

    /* The display name of the user. Depending on the user’s privacy settings, this may return an alternative value. */
    @SerialName("displayName")
    val displayName: String? = null,

    /* The email address of the user. Depending on the user’s privacy settings, this may be returned as null. */
    @SerialName("emailAddress")
    val emailAddress: String? = null,

    /* This property is no longer available and will be removed from the documentation soon. See the [deprecation notice](https://developer.atlassian.com/cloud/jira/platform/deprecation-notice-user-privacy-api-migration-guide/) for details. */
    @SerialName("key")
    val key: String? = null,

    /* This property is no longer available and will be removed from the documentation soon. See the [deprecation notice](https://developer.atlassian.com/cloud/jira/platform/deprecation-notice-user-privacy-api-migration-guide/) for details. */
    @SerialName("name")
    val name: String? = null,

    /* The URL of the user. */
    @SerialName("self")
    val self: String? = null,

    /* The time zone specified in the user's profile. Depending on the user’s privacy settings, this may be returned as null. */
    @SerialName("timeZone")
    val timeZone: String? = null

) {}
