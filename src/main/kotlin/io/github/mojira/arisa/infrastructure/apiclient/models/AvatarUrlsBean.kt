package io.github.mojira.arisa.infrastructure.apiclient.models

import URISerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AvatarUrlsBean(

    /* The URL of the item's 16x16 pixel avatar. */
    @Serializable(with = URISerializer::class)
    @SerialName("16x16")
    val `16x16`: java.net.URI? = null,

    /* The URL of the item's 24x24 pixel avatar. */
    @Serializable(with = URISerializer::class)
    @SerialName("24x24")
    val `24x24`: java.net.URI? = null,

    /* The URL of the item's 32x32 pixel avatar. */
    @Serializable(with = URISerializer::class)
    @SerialName("32x32")
    val `32x32`: java.net.URI? = null,

    /* The URL of the item's 48x48 pixel avatar. */
    @Serializable(with = URISerializer::class)
    @SerialName("48x48")
    val `48x48`: java.net.URI? = null

)
