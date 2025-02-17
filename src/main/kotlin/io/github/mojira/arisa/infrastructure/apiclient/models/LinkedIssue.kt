package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.Serializable

@Serializable
data class LinkedIssue(
    val fields: IssueFields? = null,
    val title: String? = null,
    val key: String? = null,
    val self: String? = null
)