package io.github.mojira.arisa.apiclient.models

import URISerializer
import io.github.mojira.arisa.apiclient.JiraClient
import io.github.mojira.arisa.apiclient.requestModels.CreateIssueLinkBody
import kotlinx.serialization.Serializable

@Serializable
data class IssueBean(
    val changelog: PageOf<Changelog> = PageOf(histories = emptyList()),
    val editmeta: IssueUpdateMetadata? = null,
    val expand: String? = null,
    val fields: IssueFields,
    val fieldsToInclude: IncludedFields? = null,
    val id: String,
    val key: String,
    val names: Map<String, String>? = null,
    val operations: Operations? = null,
    val properties: Map<String, String>? = null,
    val renderedFields: Map<String, String>? = null,
    val schema: Map<String, JsonTypeBean>? = null,
    @Serializable(with = URISerializer::class)
    val self: java.net.URI? = null,
    val transitions: List<IssueTransition>? = null,
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