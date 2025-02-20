package io.github.mojira.arisa.infrastructure.apiclient.builders

import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.style.StringSpec
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class FluentObjectBuilderTest : StringSpec({
    "it should allow setting simple fields" {
        // Arrange
        val update = FluentObjectBuilder()

        // Act
        update.field("summary", "New summary")
        val editBody = update.toJson().toString()

        // Assert
        editBody.shouldContainJsonKeyValue("fields.summary.name", "New summary")
    }

    "it should allow setting nested fields" {
        // Arrange
        val update = FluentObjectBuilder()

        // Act
        update.field("security") {
            subField("id", "10033")
        }
        val editBody = update.toJson().toString()

        // Assert
        editBody.shouldContainJsonKeyValue("fields.security.id", "10033")
    }

    "it should override an existing field with a new value" {
        // Arrange
        val update = FluentObjectBuilder()

        // Act
        update.field("summary", "Initial summary")
        update.field("summary", "Overridden summary")
        val editBody = update.toJson().toString()

        // Assert
        editBody.shouldContainJsonKeyValue("fields.summary.name", "Overridden summary")
    }

    "it should allow setting list of objects" {
        // Arrange
        val update = FluentObjectBuilder()

        // Act
        update.field("fixVersions", listOf(
            buildJsonObject { put("name", "v0.1.0") },
            buildJsonObject { put("name", "v0.2.0") },
        ))
        val editBody = update.toJson().toString()

        // Assert
        editBody.shouldContainJsonKeyValue("fields.fixVersions[0].name", "v0.1.0")
        editBody.shouldContainJsonKeyValue("fields.fixVersions[1].name", "v0.2.0")
    }

    "it should allow to add object to a list" {
        // Arrange
        val update = FluentObjectBuilder()

        // Act
        update.field("fixVersions", listOf(
            buildJsonObject { put("name", "v0.1.0") },
            buildJsonObject { put("name", "v0.2.0") },
        ))
        update.add("fixVersions", buildJsonObject { put("name", "v0.2.0") })

        val editBody = update.toJson().toString()

        // Assert
        editBody.shouldContainJsonKeyValue("fields.fixVersions[0].name", "v0.1.0")
        editBody.shouldContainJsonKeyValue("fields.fixVersions[1].name", "v0.2.0")
    }

    "it should allow to remove object from a list" {
        // Arrange
        val update = FluentObjectBuilder()

        // Act
        update.field("fixVersions", listOf(
            buildJsonObject { put("name", "v0.1.0") },
            buildJsonObject { put("name", "v0.2.0") },
        ))
        update.remove("fixVersions") {
            it["name"]?.jsonPrimitive?.content == "v0.1.0"
        }

        val editBody = update.toJson().toString()

        // Assert
        editBody.shouldContainJsonKeyValue("fields.fixVersions[0].name", "v0.2.0")
    }

    "it should leave the list unchanged when no element matches the remove predicate" {
        // Arrange
        val update = FluentObjectBuilder()

        // Act
        update.field("fixVersions", listOf(
            buildJsonObject { put("name", "v0.1.0") },
            buildJsonObject { put("name", "v0.2.0") },
        ))
        update.remove("fixVersions") {
            it.string("name") == "non-existent-version"
        }

        val editBody = update.toJson().toString()

        // Assert
        editBody.shouldContainJsonKeyValue("fields.fixVersions[0].name", "v0.1.0")
        editBody.shouldContainJsonKeyValue("fields.fixVersions[1].name", "v0.2.0")
    }

    "it should create a new list when adding to a non-existent field" {
        // Arrange
        val update = FluentObjectBuilder()

        // Act
        update.add("newList", buildJsonObject { put("name", "v1.0.0") })
        val editBody = update.toJson().toString()

        // Assert
        editBody.shouldContainJsonKeyValue("fields.newList[0].name", "v1.0.0")
    }
})
