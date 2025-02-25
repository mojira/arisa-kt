package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Details of user or system associated with a issue history metadata item.
 *
 * @param avatarUrl The URL to an avatar for the user or system associated with a history record.
 * @param displayName The display name of the user or system associated with a history record.
 * @param displayNameKey The key of the display name of the user or system associated with a history record.
 * @param id The ID of the user or system associated with a history record.
 * @param type The type of the user or system associated with a history record.
 * @param url The URL of the user or system associated with a history record.
 */
@Serializable
data class HistoryMetadataParticipant(
    val avatarUrl: String? = null,
    val displayName: String? = null,
    val displayNameKey: String? = null,
    val id: String? = null,
    val type: String? = null,
    val url: String? = null
) : kotlin.collections.HashMap<String, JsonElement>()
