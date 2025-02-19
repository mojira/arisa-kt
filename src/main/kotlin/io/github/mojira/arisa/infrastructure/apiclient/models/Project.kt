package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val self: String,
    val id: String,
    val avatarUrls: Map<String, String>,
    val key: String,
    val name: String,
    val description: String,
    val lead: User,
    val assigneeType: String,
//    val components: List<Component>,
//    @SerialName("issueTypes")
//    @JsonNames("issuetypes")
//    val issueTypes: List<IssueType>,
    val versions: List<Version>,
    val roles: Map<String, String>,
//    val category: ProjectCategory,
    val email: String? = null
)
