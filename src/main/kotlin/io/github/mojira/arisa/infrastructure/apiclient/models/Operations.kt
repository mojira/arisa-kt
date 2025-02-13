package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Details of the operations that can be performed on the issue.
 *
 * @param linkGroups Details of the link groups defining issue operations.
 */
@Serializable
data class Operations(

    /* Details of the link groups defining issue operations. */
    @SerialName("linkGroups")
    val linkGroups: List<LinkGroup>? = null

)
