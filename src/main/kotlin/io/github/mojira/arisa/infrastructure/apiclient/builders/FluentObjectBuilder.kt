package io.github.mojira.arisa.infrastructure.apiclient.builders

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.content
fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.int
fun JsonObject.bool(key: String): Boolean? = this[key]?.jsonPrimitive?.boolean

class FluentObjectBuilder {
    private val fields = mutableMapOf<String, JsonElement>()

    fun field(name: String, value: String): FluentObjectBuilder {
        fields[name] = buildJsonObject { put("name", Json.encodeToJsonElement(value)) }
        return this
    }

    fun field(name: String, value: Double): FluentObjectBuilder {
        fields[name] = buildJsonObject { put("name", Json.encodeToJsonElement(value)) }
        return this
    }

    fun field(name: String, init: FieldBuilder.() -> Unit): FluentObjectBuilder {
        val builder = FieldBuilder().apply(init)
        fields[name] = builder.build()
        return this
    }

    fun field(name: String, value: List<JsonObject>): FluentObjectBuilder {
        fields[name] = Json.encodeToJsonElement(value)
        return this
    }

    fun add(fieldName: String, element: JsonObject): FluentObjectBuilder {
        val list = getListField(fieldName)
        list.add(element)
        fields[fieldName] = Json.encodeToJsonElement(list)
        return this
    }

    fun remove(fieldName: String, predicate: (JsonObject) -> Boolean): FluentObjectBuilder {
        val list = getListField(fieldName)
        list.removeAll(predicate)
        fields[fieldName] = Json.encodeToJsonElement(list)
        return this
    }

    private fun getListField(fieldName: String): MutableList<JsonObject> {
        val existing = fields[fieldName]
        return if (existing is JsonArray) {
            existing.mapNotNull { if (it is JsonObject) it else null }.toMutableList()
        } else {
            mutableListOf()
        }
    }

    // Returns the overall JSON object with the "fields" key.
    fun toJson(): JsonObject = buildJsonObject {
        put("fields", Json.encodeToJsonElement(fields))
    }
}