package io.github.mojira.arisa.infrastructure.apiclient.builders

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

class FieldBuilderTest : StringSpec({
    "build should return a JsonObject with string, int, and boolean fields" {
        val builder = FieldBuilder()
        builder.subField("name", "John")
        builder.subField("age", 30)
        builder.subField("isActive", true)

        val result: JsonObject = builder.build()
        result["name"]?.jsonPrimitive?.content shouldBe "John"
        result["age"]?.jsonPrimitive?.int shouldBe 30
        result["isActive"]?.jsonPrimitive?.boolean shouldBe true
    }
})
