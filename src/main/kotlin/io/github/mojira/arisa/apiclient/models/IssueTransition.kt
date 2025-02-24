package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Details of an issue transition.
 *
 * @param expand Expand options that include additional transition details in the response.
 * @param fields Details of the fields associated with the issue transition screen. Use this
 *               information to populate `fields` and `update` in a transition request.
 * @param hasScreen Whether there is a screen associated with the issue transition.
 * @param id The ID of the issue transition. Required when specifying a transition to undertake.
 * @param isAvailable Whether the transition is available to be performed.
 * @param isConditional Whether the issue has to meet criteria before the issue transition is
 *                      applied.
 * @param isGlobal Whether the issue transition is global, that is, the transition is applied to
 *                 issues regardless of their status.
 * @param isInitial Whether this is the initial issue transition for the workflow.
 * @param looped
 * @param name The name of the issue transition.
 * @param to Details of the issue status after the transition.
 */
@Serializable
data class IssueTransition(
    val expand: String? = null,
    val fields: Map<String, FieldMetadata>? = null,
    val hasScreen: Boolean? = null,
    val id: String? = null,
    val isAvailable: Boolean? = null,
    val isConditional: Boolean? = null,
    val isGlobal: Boolean? = null,
    val isInitial: Boolean? = null,
    val looped: Boolean? = null,
    val name: String? = null,
    @SerialName("to")
    val toStatus: StatusDetails? = null
)
