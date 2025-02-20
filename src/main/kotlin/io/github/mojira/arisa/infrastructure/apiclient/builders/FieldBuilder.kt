package io.github.mojira.arisa.infrastructure.apiclient.builders

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement

class FieldBuilder {
    private val content = mutableMapOf<String, JsonElement>()

    fun subField(key: String, value: String) {
        content[key] = Json.encodeToJsonElement(value)
    }

    fun subField(key: String, value: Int) {
        content[key] = Json.encodeToJsonElement(value)
    }

    fun subField(key: String, value: Boolean) {
        content[key] = Json.encodeToJsonElement(value)
    }

    fun build(): JsonObject = buildJsonObject {
        content.forEach { (key, value) -> put(key, value) }
    }
}
