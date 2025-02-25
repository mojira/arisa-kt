package io.github.mojira.arisa.apiclient.models

import kotlinx.serialization.Serializable

@Serializable
data class IssueLink(
    val id: String? = null,
    val inwardIssue: IssueBean? = null,
    val outwardIssue: IssueBean? = null,
    val self: String? = null,
    val type: IssueLinkType? = null
)
