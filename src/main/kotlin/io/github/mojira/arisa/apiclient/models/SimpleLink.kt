package io.github.mojira.arisa.apiclient.models

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
    val href: String? = null,
    val iconClass: String? = null,
    val id: String? = null,
    val label: String? = null,
    val styleClass: String? = null,
    val title: String? = null,
    val weight: Int? = null
)
