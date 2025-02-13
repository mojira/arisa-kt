package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.SerialName
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

    /* The URL to an avatar for the user or system associated with a history record. */
    @SerialName("avatarUrl")
    val avatarUrl: String? = null,

    /* The display name of the user or system associated with a history record. */
    @SerialName("displayName")
    val displayName: String? = null,

    /* The key of the display name of the user or system associated with a history record. */
    @SerialName("displayNameKey")
    val displayNameKey: String? = null,

    /* The ID of the user or system associated with a history record. */
    @SerialName("id")
    val id: String? = null,

    /* The type of the user or system associated with a history record. */
    @SerialName("type")
    val type: String? = null,

    /* The URL of the user or system associated with a history record. */
    @SerialName("url")
    val url: String? = null

) : kotlin.collections.HashMap<String, JsonElement>()
