package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User details permitted by the user's Atlassian Account privacy settings. However, be
 * aware of these exceptions:
 *   - User record deleted from Atlassian: This occurs as the result of a right to be
 *     forgotten request. In this case, `displayName` provides an indication and other
 *     parameters have default values or are blank (for example, email is blank).
 *   - User record corrupted: This occurs as a result of events such as a server import and
 *     can only happen to deleted users. In this case, `accountId` returns *unknown* and all
 *     other parameters have fallback values.
 *   - User record unavailable: This usually occurs due to an internal service outage. In this
 *     case, all parameters have fallback values.
 *
 * @param accountId The account ID of the user, which uniquely identifies the user across all
 * Atlassian products. For example, *5b10ac8d82e05b22cc7d4ef5*.
 * @param accountType The type of account represented by this user. This will be one of
 * 'atlassian' (normal users), 'app' (application user) or 'customer' (Jira Service Desk customer
 * user).
 * @param active Whether the user is active.
 * @param avatarUrls The avatars of the user.
 * @param displayName The display name of the user. Depending on the user’s privacy settings,
 * this may return an alternative value.
 * @param emailAddress The email address of the user. Depending on the user’s privacy settings,
 * this may be returned as null.
 * @param key This property is no longer available and will be removed from the documentation soon.
 * See the [deprecation notice](
 *   https://developer.atlassian.com/cloud/jira/platform/deprecation-notice-user-privacy-api-migration-guide/)
 * for details.
 * @param name This property is no longer available and will be removed from the documentation soon.
 * See the [deprecation notice](
 *   https://developer.atlassian.com/cloud/jira/platform/deprecation-notice-user-privacy-api-migration-guide/)
 * for details.
 * @param self The URL of the user.
 * @param timeZone The time zone specified in the user's profile. Depending on the user’s privacy
 * settings, this may be returned as null.
 */
@Serializable
data class UserDetails(
    @SerialName("accountId")
    val accountId: String? = null,
    @SerialName("accountType")
    val accountType: String? = null,
    @SerialName("active")
    val active: Boolean? = null,
    @SerialName("avatarUrls")
    val avatarUrls: AvatarUrlsBean? = null,
    @SerialName("displayName")
    val displayName: String? = null,
    @SerialName("emailAddress")
    val emailAddress: String? = null,
    @SerialName("key")
    val key: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("self")
    val self: String? = null,
    @SerialName("timeZone")
    val timeZone: String? = null
)
