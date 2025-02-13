package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Details about the operations available in this version.
 *
 * @param href
 * @param iconClass
 * @param id
 * @param label
 * @param styleClass
 * @param title
 * @param weight
 */
@Serializable
data class SimpleLink(

    @SerialName("href")
    val href: String? = null,

    @SerialName("iconClass")
    val iconClass: String? = null,

    @SerialName("id")
    val id: String? = null,

    @SerialName("label")
    val label: String? = null,

    @SerialName("styleClass")
    val styleClass: String? = null,

    @SerialName("title")
    val title: String? = null,

    @SerialName("weight")
    val weight: Int? = null

)
