package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A page of changelogs.
 *
 * @param histories The list of changelogs.
 * @param maxResults The maximum number of results that could be on the page.
 * @param startAt The index of the first item returned on the page.
 * @param total The number of results on the page.
 */
@Serializable
data class PageOf<TItem>(
    /* The list of changelogs. */
    @SerialName("histories")
    val histories: List<TItem>? = null,

    /* The maximum number of results that could be on the page. */
    @SerialName("maxResults")
    val maxResults: Int? = null,

    /* The index of the first item returned on the page. */
    @SerialName("startAt")
    val startAt: Int? = null,

    /* The number of results on the page. */
    @SerialName("total")
    val total: Int? = null
)
