package io.github.mojira.arisa.infrastructure.apiclient.models

import kotlinx.serialization.json.Json
import java.io.File

fun loadJsonFile(filename: String): String {
    val file = File("src/test/resources/responses/$filename")
    return file.readText()
}

val JSON = Json { ignoreUnknownKeys = true }
