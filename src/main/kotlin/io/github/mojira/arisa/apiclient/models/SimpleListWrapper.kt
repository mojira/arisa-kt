package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *
 * @param callback
 * @param items
 * @param maxResults
 * @param pagingCallback
 * @param propertySize
 */
@Serializable
data class SimpleListWrapper<TItem>(
//    @SerialName("callback")
//    val callback: Any? = null,
    @SerialName("items")
    val items: List<TItem>? = null,
    @SerialName("max-results")
    val maxResults: Int? = null,
//    @SerialName("pagingCallback")
//    val pagingCallback: Any? = null,
    @SerialName("size")
    val propertySize: Int? = null
)
