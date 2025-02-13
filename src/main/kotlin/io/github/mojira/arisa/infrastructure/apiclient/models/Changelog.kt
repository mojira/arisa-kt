package io.github.mojira.arisa.infrastructure.apiclient.models

import io.github.mojira.arisa.infrastructure.apiclient.serializers.OffsetDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A log of changes made to issue fields. Changelogs related to workflow associations are currently being deprecated.
 *
 * @param author The user who made the change.
 * @param created The date on which the change took place.
 * @param historyMetadata The history metadata associated with the changed.
 * @param id The ID of the changelog.
 * @param items The list of items changed.
 */
@Serializable
data class Changelog (

    /* The user who made the change. */
    @SerialName("author")
    val author: UserDetails? = null,

    /* The date on which the change took place. */
    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("created")
    val created: java.time.OffsetDateTime? = null,

    /* The history metadata associated with the changed. */
    @SerialName("historyMetadata")
    val historyMetadata: HistoryMetadata? = null,

    /* The ID of the changelog. */
    @SerialName("id")
    val id: kotlin.String? = null,

    /* The list of items changed. */
    @SerialName("items")
    val items: kotlin.collections.List<ChangeDetails>? = null

) {}
