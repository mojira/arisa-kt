package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The projects the item is associated with. Indicated for items associated with next-gen projects.
 *
 * @param project The project the item has scope in.
 * @param type The type of scope.
 */
@Serializable
data class Scope(
    /* The project the item has scope in. */
    @SerialName("project")
    val project: ProjectDetails? = null,

    /* The type of scope. */
    @SerialName("type")
    val type: Type? = null
) {
    /**
     * The type of scope.
     *
     * Values: PROJECT,TEMPLATE
     */
    @Serializable
    enum class Type(val value: String) {
        @SerialName("PROJECT")
        PROJECT("PROJECT"),

        @SerialName("TEMPLATE")
        TEMPLATE("TEMPLATE")
    }
}
