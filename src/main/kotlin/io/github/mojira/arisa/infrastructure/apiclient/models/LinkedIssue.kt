package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.Serializable

@Serializable
data class LinkedIssue(
    val fields: IssueFields,
    val title: String,
    val key: String,
    val self: String
)