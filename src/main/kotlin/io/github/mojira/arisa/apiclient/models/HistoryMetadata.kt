package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Details of issue history metadata.
 *
 * @param activityDescription The activity described in the history record.
 * @param activityDescriptionKey The key of the activity described in the history record.
 * @param actor Details of the user whose action created the history record.
 * @param cause Details of the cause that triggered the creation the history record.
 * @param description The description of the history record.
 * @param descriptionKey The description key of the history record.
 * @param emailDescription The description of the email address associated the history record.
 * @param emailDescriptionKey The description key of the email address associated the history record.
 * @param extraData Additional arbitrary information about the history record.
 * @param generator Details of the system that generated the history record.
 * @param type The type of the history record.
 */
@Serializable
data class HistoryMetadata(
    val activityDescription: String? = null,
    val activityDescriptionKey: String? = null,
    val actor: HistoryMetadataParticipant? = null,
    val cause: HistoryMetadataParticipant? = null,
    val description: String? = null,
    val descriptionKey: String? = null,
    val emailDescription: String? = null,
    val emailDescriptionKey: String? = null,
    val extraData: Map<String, String>? = null,
    val generator: HistoryMetadataParticipant? = null,
    val type: String? = null

) : kotlin.collections.HashMap<String, JsonElement>()
