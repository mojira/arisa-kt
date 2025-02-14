package io.github.mojira.arisa.infrastructure.apiclient.models

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
 */
@Serializable
data class User(
    /* The account ID of the user, which uniquely identifies the user across all Atlassian products.
       For example, *5b10ac8d82e05b22cc7d4ef5*. Required in requests. */
    @SerialName("accountId")
    val accountId: String? = null,
    /* The user account type. Can take the following values:
     *  `atlassian` regular Atlassian user account
     *  `app` system account used for Connect applications and OAuth to represent external systems
     *  `customer` Jira Service Desk account representing an external service desk */
    @SerialName("accountType")
    val accountType: AccountType? = null,
    // Whether the user is active.
    @SerialName("active")
    val active: Boolean? = null,
    // The application roles the user is assigned to.
    @SerialName("applicationRoles")
    val applicationRoles: SimpleListWrapper<ApplicationRole>? = null,
    // The avatars of the user.
    @SerialName("avatarUrls")
    val avatarUrls: AvatarUrlsBean? = null,
    // The display name of the user. Depending on the user's privacy setting, this may return an alternative value.
    @SerialName("displayName")
    val displayName: String? = null,
    // The email address of the user. Depending on the user's privacy setting, this may be returned as null.
    @SerialName("emailAddress")
    val emailAddress: String? = null,
    // Expand options that include additional user details in the response.
    @SerialName("expand")
    val expand: String? = null,
    // The groups that the user belongs to.
    @SerialName("groups")
    val groups: SimpleListWrapper<GroupName>? = null,
    // This property is no longer available and will be removed from the documentation soon.
    @SerialName("key")
    val key: String? = null,
    // The locale of the user. Depending on the user's privacy setting, this may be returned as null.
    @SerialName("locale")
    val locale: String? = null,
    // This property is no longer available and will be removed from the documentation soon.
    @SerialName("name")
    val name: String? = null,
    // The URL of the user.
    @Serializable(with = URISerializer::class)
    @SerialName("self")
    val self: URI? = null,
    // The time zone specified in the user's profile. Depending on the user's privacy setting, this may be returned as
    // null.
    @SerialName("timeZone")
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
