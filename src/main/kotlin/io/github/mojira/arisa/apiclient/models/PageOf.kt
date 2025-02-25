package io.github.mojira.arisa.apiclient.models

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
    val histories: List<TItem>? = null,
    val maxResults: Int? = null,
    val startAt: Int? = null,
    val total: Int? = null
)
