package io.github.mojira.arisa.apiclient.requestModels

import io.github.mojira.arisa.apiclient.models.IssueLink
import io.github.mojira.arisa.apiclient.models.IssueLinkType
import kotlinx.serialization.Serializable

@Serializable
data class CreateIssueLinkBody(
    val inwardIssue: IssueLink,
    val outwardIssue: IssueLink,
    val type: IssueLinkType
)
