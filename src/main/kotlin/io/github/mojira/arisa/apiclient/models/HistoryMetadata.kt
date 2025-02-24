package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.SerialName
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

    /* The activity described in the history record. */
    @SerialName("activityDescription")
    val activityDescription: String? = null,

    /* The key of the activity described in the history record. */
    @SerialName("activityDescriptionKey")
    val activityDescriptionKey: String? = null,

    /* Details of the user whose action created the history record. */
    @SerialName("actor")
    val actor: HistoryMetadataParticipant? = null,

    /* Details of the cause that triggered the creation the history record. */
    @SerialName("cause")
    val cause: HistoryMetadataParticipant? = null,

    /* The description of the history record. */
    @SerialName("description")
    val description: String? = null,

    /* The description key of the history record. */
    @SerialName("descriptionKey")
    val descriptionKey: String? = null,

    /* The description of the email address associated the history record. */
    @SerialName("emailDescription")
    val emailDescription: String? = null,

    /* The description key of the email address associated the history record. */
    @SerialName("emailDescriptionKey")
    val emailDescriptionKey: String? = null,

    /* Additional arbitrary information about the history record. */
    @SerialName("extraData")
    val extraData: Map<String, String>? = null,

    /* Details of the system that generated the history record. */
    @SerialName("generator")
    val generator: HistoryMetadataParticipant? = null,

    /* The type of the history record. */
    @SerialName("type")
    val type: String? = null

) : kotlin.collections.HashMap<String, JsonElement>()
