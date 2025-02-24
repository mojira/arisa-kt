package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param actuallyIncluded
 * @param excluded
 * @param included
 */
@Serializable
data class IncludedFields(

    @SerialName("actuallyIncluded")
    val actuallyIncluded: Set<String>? = null,

    @SerialName("excluded")
    val excluded: Set<String>? = null,

    @SerialName("included")
    val included: Set<String>? = null

)
