package io.github.mojira.arisa.apiclient.models

import URISerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URI

/**
 * A user with details as permitted by the user's Atlassian Account privacy settings. However, be aware of these
 * exceptions:
 * *  User record deleted from Atlassian: This occurs as the result of a right to be forgotten request. In this case,
 * `displayName` provides an indication and other parameters have default values or are blank (for example, email is
 * blank).
 * *  User record corrupted: This occurs as a results of events such as a server import and can only happen to deleted
 * users. In this case, `accountId` returns *unknown* and all other parameters have fallback values.
 * *  User record unavailable: This usually occurs due to an internal service outage. In this case, all parameters have
 * fallback values.
 *
 * @param accountId The account ID of the user, which uniquely identifies the user across all Atlassian products.
 * For example, *5b10ac8d82e05b22cc7d4ef5*. Required in requests.
 * @param accountType The user account type. Can take the following values:
 *  *  `atlassian` regular Atlassian user account
 *  *  `app` system account used for Connect applications and OAuth to represent external systems
 *  *  `customer` Jira Service Desk account representing an external service desk
 * @param active Whether the user is active.
 * @param applicationRoles The application roles the user is assigned to.
 * @param avatarUrls The avatars of the user.
 * @param displayName The display name of the user. Depending on the user's privacy setting, this may return an alternative value.
 * @param emailAddress The email address of the user. Depending on the user's privacy setting, this may be returned as null.
 * @param expand Expand options that include additional user details in the response.
 * @param groups The groups that the user belongs to.
 * @param key This property is no longer available and will be removed from the documentation soon.
 * @param locale The locale of the user. Depending on the user's privacy setting, this may be returned as null.
 * @param name This property is no longer available and will be removed from the documentation soon.
 * @param self The URL of the user.
 * @param timeZone The time zone specified in the user's profile. Depending on the user's privacy setting, this may be returned as null.
 */
@Serializable
data class User(
    val accountId: String,
    val accountType: AccountType? = null,
    val active: Boolean = false,
    val applicationRoles: SimpleListWrapper<ApplicationRole>? = null,
    val avatarUrls: AvatarUrlsBean? = null,
    val displayName: String? = null,
    val emailAddress: String? = null,
    val expand: String? = null,
    val groups: SimpleListWrapper<GroupName>? = null,
    val key: String? = null,
    val locale: String? = null,
    val name: String? = null,
    @Serializable(with = URISerializer::class)
    val self: URI? = null,
    val timeZone: String? = null
) {
    /**
     * The user account type.
     */
    @Serializable
    enum class AccountType(
        val value: String
    ) {
        @SerialName("atlassian")
        Atlassian("atlassian"),

        @SerialName("app")
        App("app"),

        @SerialName("customer")
        Customer("customer"),

        @SerialName("unknown")
        Unknown("unknown")
    }
}
