package io.github.mojira.arisa.infrastructure.apiclient.requestModels

import io.github.mojira.arisa.infrastructure.apiclient.models.IssueLink
import io.github.mojira.arisa.infrastructure.apiclient.models.IssueLinkType
import kotlinx.serialization.Serializable

@Serializable
data class CreateIssueLinkBody(
    val inwardIssue: IssueLink,
    val outwardIssue: IssueLink,
    val type: IssueLinkType
)
