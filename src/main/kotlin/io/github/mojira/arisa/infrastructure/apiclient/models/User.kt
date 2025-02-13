package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AccountType(val value: String) {
    atlassian("atlassian"),
    app("app"),
    customer("customer"),
    unknown("unknown")
}

@Serializable
data class User(

    @SerialName("accountId")
    val accountId: String? = null,

    @SerialName("accountType")
    val accountType: AccountType? = null,

    @SerialName("active")
    val active: Boolean? = null,

//    @SerialName("applicationRoles")
//    val applicationRoles: SimpleListWrapperApplicationRole? = null,

//    @SerialName("avatarUrls")
//    val avatarUrls: AvatarUrlsBean? = null,
) {}
