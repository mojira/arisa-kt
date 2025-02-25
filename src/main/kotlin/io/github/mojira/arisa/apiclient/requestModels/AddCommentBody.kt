package io.github.mojira.arisa.apiclient.requestModels

import io.github.mojira.arisa.apiclient.models.BodyType
import io.github.mojira.arisa.apiclient.models.EntityProperty
import io.github.mojira.arisa.apiclient.models.Visibility
import kotlinx.serialization.Serializable

@Serializable
data class AddCommentBody(
    val body: BodyType,
    val properties: List<EntityProperty>? = null,
    val visibility: Visibility? = null
) : HashMap<String, String>()

@Serializable
data class UpdateCommentBody(
    val body: BodyType,
    val properties: List<EntityProperty>? = null,
    val visibility: Visibility? = null
) : HashMap<String, String>()
