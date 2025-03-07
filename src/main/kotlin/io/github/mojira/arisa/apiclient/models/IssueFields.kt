package io.github.mojira.arisa.apiclient.models

import io.github.mojira.arisa.apiclient.serializers.OffsetDateTimeSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import java.time.OffsetDateTime

@Serializable
data class IssueFields(
    val aggregatetimespent: Long? = null,
    val assignee: UserDetails? = null,
    val attachment: List<AttachmentBean> = emptyList(),
    val comment: CommentContainer? = null,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val created: OffsetDateTime? = null,
    val creator: User? = null,
    val description: String? = null,
    val duedate: String? = null,
    val environment: String? = null,
    val issuelinks: List<IssueLink> = emptyList(),
    val issuerestriction: IssueRestriction? = null,
    val issueType: IssueType? = null,
    val labels: List<String>? = null,
    val lastViewed: String? = null,
    val priority: IssuePriority? = null,
    val reporter: User? = null,
    val resolution: Resolution? = null,
    val resolutiondate: String? = null,
    val security: IssueSecurity? = null,
    val status: IssueStatus? = null,
    val statuscategorychangedate: String? = null,
    val subtasks: List<IssueBean>? = null,
    val summary: String? = null,
    val versions: List<JsonElement> = emptyList(),
    val timeoriginalestimate: JsonElement? = null,
    val timespent: Long? = null,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val updated: OffsetDateTime? = null,
    val workratio: Int? = null,
    @Transient
    private val additionalProperties: Map<String, JsonElement> = emptyMap()
) {
    operator fun get(field: String): JsonElement? {
        return additionalProperties[field]
    }
}

@Serializable
data class CommentContainer(
    val comments: List<Comment> = emptyList(),
    val self: String?,
    val maxResults: Int?,
    val total: Int?,
    val startAt: Int?
)

@Serializable
data class IssueRestriction(
    val issuerestrictions: JsonElement,
    val shouldDisplay: Boolean
)
