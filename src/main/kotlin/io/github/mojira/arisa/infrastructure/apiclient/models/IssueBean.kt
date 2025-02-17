package io.github.mojira.arisa.infrastructure.apiclient.models

import URISerializer
import io.github.mojira.arisa.infrastructure.apiclient.OpenApiObject
import io.github.mojira.arisa.infrastructure.apiclient.requestModels.EditIssueBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class IssueBean(
    // Details of changelogs associated with the issue.
    @SerialName("changelog")
    val changelog: PageOf<Changelog>? = null,
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

    /* The attachments associated with the issue. */
    @SerialName("attachments")
    val attachments: List<Attachment>? = null
) {
    companion object {
        data class FieldOperation(
            val operation: String,
            val value: Any
        )
    }

    inner class FluentUpdate {
        private val fields = mutableMapOf<String, JsonElement>()
        private val fieldOpers = mutableMapOf<String, MutableList<JsonObject>>()

        /**
         * Appends a field to the update action.
         */
        fun field(name: String, value: Any): FluentUpdate {
            fields[name] = Json.encodeToJsonElement(value)
            return this
        }

        /**
         * Adds a field value to the existing value set.
         */
        fun fieldAdd(name: String, value: Any): FluentUpdate {
            return fieldOperation(name, "add", value)
        }

        /**
         * Removes a field value from the existing value set.
         */
        fun fieldRemove(name: String, value: Any): FluentUpdate {
            return fieldOperation(name, "remove", value)
        }

        /**
         * Sets a field value
         */
        fun fieldSet(name: String, value: Any): FluentUpdate {
            return fieldOperation(name, "set", value)
        }

        /**
         * Edits a field value with complex structure
         */
        fun fieldEdit(name: String, value: Map<String, Any>): FluentUpdate {
            return fieldOperation(name, "edit", value)
        }

        private fun fieldOperation(name: String, operation: String, value: Any): FluentUpdate {
            val operationObject = buildJsonObject {
                put(operation, Json.encodeToJsonElement(value))
            }

            fieldOpers.getOrPut(name) { mutableListOf() }.add(operationObject)
            return this
        }

        /**
         * Executes the update action and returns both the request body and updated IssueBean
         */
        fun execute(): Pair<EditIssueBody, IssueBean> {
            if (fields.isEmpty() && fieldOpers.isEmpty()) {
                throw IllegalStateException("No fields were given for update")
            }

            val updateObject = buildJsonObject {
                fieldOpers.forEach { (field, operations) ->
                    put(field, JsonArray(operations))
                }
            }

            val requestBody = EditIssueBody(
                fields = if (fields.isNotEmpty()) fields else null,
                update = if (fieldOpers.isNotEmpty()) updateObject.toMap() else null
            )

            // Create updated IssueBean
            val updatedFields = (this@IssueBean.fields as? Map<String, JsonElement>)?.toMutableMap()
                ?: mutableMapOf()
            fields.forEach { (name, value) ->
                updatedFields[name] = value
            }

            val updatedIssue = this@IssueBean.copy(
                fields = updatedFields as IssueFields
            )

            return Pair(requestBody, updatedIssue)
        }
    }

    /**
     * Creates a new FluentUpdate instance for this issue
     */
    fun update(): FluentUpdate = FluentUpdate()
}
