package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.Serializable

@Serializable
data class IssueLink(
    val id: String? = null,
    val inwardIssue: LinkedIssue? = null,
    val outwardIssue: LinkedIssue? = null,
    val self: String? = null,
    val type: IssueLinkType? = null,
)