package io.github.mojira.arisa.infrastructure.apiclient.models

import URISerializer
import io.github.mojira.arisa.infrastructure.apiclient.OpenApiObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IssueBean(
    // Details of changelogs associated with the issue.
    @SerialName("changelog")
    val changelog: PageOfChangelogs? = null,
    // The metadata for the fields on the issue that can be amended.
    @SerialName("editmeta")
    val editmeta: IssueUpdateMetadata? = null,
    // Expand options that include additional issue details in the response.
    @SerialName("expand")
    val expand: String? = null,
    @SerialName("fields")
    val fields: OpenApiObject? = null,
    @SerialName("fieldsToInclude")
    val fieldsToInclude: IncludedFields? = null,
    // The ID of the issue.
    @SerialName("id")
    val id: String? = null,
    // The key of the issue.
    @SerialName("key")
    val key: String? = null,
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
    val versionedRepresentations: Map<String, Map<String, String>>? = null
)
