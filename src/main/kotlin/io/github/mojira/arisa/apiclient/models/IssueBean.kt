package io.github.mojira.arisa.apiclient.models

import URISerializer
import io.github.mojira.arisa.apiclient.JiraClient
import io.github.mojira.arisa.apiclient.requestModels.CreateIssueLinkBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IssueBean(
    // Details of changelogs associated with the issue.
    @SerialName("changelog")
    val changelog: PageOf<Changelog> = PageOf(histories = emptyList()),
    // The metadata for the fields on the issue that can be amended.
    @SerialName("editmeta")
    val editmeta: IssueUpdateMetadata? = null,
    // Expand options that include additional issue details in the response.
    @SerialName("expand")
    val expand: String? = null,
    @SerialName("fields")
    val fields: IssueFields,
    @SerialName("fieldsToInclude")
    val fieldsToInclude: IncludedFields? = null,
    // The ID of the issue.
    @SerialName("id")
    val id: String,
    // The key of the issue.
    @SerialName("key")
    val key: String,
    // The ID and name of each field present on the issue.
    @SerialName("names")
    val names: Map<String, String>? = null,
    // The operations that can be performed on the issue.
    @SerialName("operations")
    val operations: Operations? = null,
    // Details of the issue properties identified in the request.
    @SerialName("properties")
    val properties: Map<String, String>? = null,
    // The rendered value of each field present on the issue.
    @SerialName("renderedFields")
    val renderedFields: Map<String, String>? = null,
    // The schema describing each field present on the issue.
    @SerialName("schema")
    val schema: Map<String, JsonTypeBean>? = null,
    // The URL of the issue details.
    @Serializable(with = URISerializer::class)
    @SerialName("self")
    val self: java.net.URI? = null,
    // The transitions that can be performed on the issue.
    @SerialName("transitions")
    val transitions: List<IssueTransition>? = null,
    // The versions of each field on the issue.
    @SerialName("versionedRepresentations")
    val versionedRepresentations: Map<String, Map<String, String>>? = null,
) {
    fun link(client: JiraClient, issueId: String, linkType: String) {
        val link = CreateIssueLinkBody(
            type = IssueLinkType(name = linkType),
            inwardIssue = IssueLink(id = issueId),
            outwardIssue = IssueLink(id = this.id)
        )

        client.createIssueLink(link)
    }
}
