package io.github.mojira.arisa.apiclient.models

import io.github.mojira.arisa.apiclient.serializers.OffsetDateTimeSerializer
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
data class Changelog(
    val author: UserDetails? = null,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val created: java.time.OffsetDateTime,
    val historyMetadata: HistoryMetadata? = null,
    val id: String? = null,
    val items: List<ChangeDetails> = emptyList()
)
