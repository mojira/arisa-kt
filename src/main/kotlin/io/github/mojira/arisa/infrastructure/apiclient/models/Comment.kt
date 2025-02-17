@file:Suppress("MaxLineLength")

package io.github.mojira.arisa.infrastructure.apiclient.models

import io.github.mojira.arisa.infrastructure.apiclient.serializers.OffsetDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A comment.
 *
 * @param author The ID of the user who created the comment.
 * @param body The comment text in [Atlassian Document Format](https://developer.atlassian.com/cloud/jira/platform/apis/document/structure/).
 * @param created The date and time at which the comment was created.
 * @param id The ID of the comment.
 * @param jsdAuthorCanSeeRequest Whether the comment was added from an email sent by a person who is not part of the issue. See [Allow external emails to be added as comments on issues](https://support.atlassian.com/jira-service-management-cloud/docs/allow-external-emails-to-be-added-as-comments-on-issues/)for information on setting up this feature.
 * @param jsdPublic Whether the comment is visible in Jira Service Desk. Defaults to true when comments are created
 * in the Jira Cloud Platform. This includes when the site doesn't use Jira Service Desk or the project isn't
 * a Jira Service Desk project and, therefore, there is no Jira Service Desk for the issue to be visible on.
 * To create a comment with its visibility in Jira Service Desk set to false, use the Jira Service Desk REST API [Create request comment](https://developer.atlassian.com/cloud/jira/service-desk/rest/#api-rest-servicedeskapi-request-issueIdOrKey-comment-post) operation.
 * @param properties A list of comment properties. Optional on create and update.
 * @param renderedBody The rendered version of the comment.
 * @param self The URL of the comment.
 * @param updateAuthor The ID of the user who updated the comment last.
 * @param updated The date and time at which the comment was updated last.
 * @param visibility The group or role to which this comment is visible. Optional on create and update.
 */
@Serializable
data class Comment(
    // The ID of the user who created the comment.
    @SerialName("author")
    val author: UserDetails? = null,
    // The comment text in [Atlassian Document Format](https://developer.atlassian.com/cloud/jira/platform/apis/document/structure/).
    @SerialName("body")
    val body: BodyType,
    // The date and time at which the comment was created.
    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("created")
    val created: java.time.OffsetDateTime? = null,
    // The ID of the comment.
    @SerialName("id")
    val id: String,
    // Whether the comment was added from an email sent by a person who is not part of the issue. See [Allow external emails to be added as comments on issues](https://support.atlassian.com/jira-service-management-cloud/docs/allow-external-emails-to-be-added-as-comments-on-issues/)for information on setting up this feature.
    @SerialName("jsdAuthorCanSeeRequest")
    val jsdAuthorCanSeeRequest: Boolean? = null,
    @SerialName("jsdPublic")
    val jsdPublic: Boolean? = null,
    // A list of comment properties. Optional on create and update.
    @SerialName("properties")
    val properties: List<EntityProperty>? = null,
    // The rendered version of the comment.
    @SerialName("renderedBody")
    val renderedBody: String? = null,
    // The URL of the comment.
    @SerialName("self")
    val self: String? = null,
    // The ID of the user who updated the comment last.
    @SerialName("updateAuthor")
    val updateAuthor: UserDetails? = null,
    // The date and time at which the comment was updated last.
    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("updated")
    val updated: java.time.OffsetDateTime? = null,
    // The group or role to which this comment is visible. Optional on create and update.
    @SerialName("visibility")
    val visibility: Visibility? = null
)
