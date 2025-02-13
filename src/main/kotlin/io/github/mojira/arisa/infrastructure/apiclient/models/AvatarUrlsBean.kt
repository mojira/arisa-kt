package io.github.mojira.arisa.infrastructure.apiclient.models

import URISerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A bean representing the avatar URLs of an item in various sizes.
 *
 * @property size16 The URL of the item's 16x16 pixel avatar.
 * @property size24 The URL of the item's 24x24 pixel avatar.
 * @property size32 The URL of the item's 32x32 pixel avatar.
 * @property size48 The URL of the item's 48x48 pixel avatar.
 */
@Serializable
data class AvatarUrlsBean(
    @Serializable(with = URISerializer::class)
    @SerialName("16x16")
    val size16: java.net.URI? = null,
    @Serializable(with = URISerializer::class)
    @SerialName("24x24")
    val size24: java.net.URI? = null,
    @Serializable(with = URISerializer::class)
    @SerialName("32x32")
    val size32: java.net.URI? = null,
    @Serializable(with = URISerializer::class)
    @SerialName("48x48")
    val size48: java.net.URI? = null
)
