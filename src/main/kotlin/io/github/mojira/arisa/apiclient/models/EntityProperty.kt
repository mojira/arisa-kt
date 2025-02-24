package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An entity property, for more information see [Entity properties](https://developer.atlassian.com/cloud/jira/platform/jira-entity-properties/).
 *
 * @param key The key of the property. Required on create and update.
 * @param `value` The value of the property. Required on create and update.
 */
@Serializable
data class EntityProperty(
    /* The key of the property. Required on create and update. */
    @SerialName("key")
    val key: String? = null,

    /* The value of the property. Required on create and update. */
    @SerialName("value")
    val `value`: String? = null
//    val `value`: Any? = null
)
