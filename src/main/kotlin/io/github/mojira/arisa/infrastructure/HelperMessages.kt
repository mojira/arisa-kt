package io.github.mojira.arisa.infrastructure

import arrow.core.Either
import arrow.core.rightIfNotNull
import com.beust.klaxon.Klaxon
import java.net.URL
import java.net.URLConnection

typealias ProjectFilter = Any
typealias LocalizedValues = Map<String, String>

data class HelperMessages(
    val variables: Map<String, List<Variable>>,
    val messages: Map<String, List<Message>>
) {
    data class Variable(
        val project: ProjectFilter,
        val value: String,
        val localizedValues: LocalizedValues? = null
    )

    data class Message(
        val project: ProjectFilter,
        val name: String,
        val message: String,
        val fillname: List<String>,
        val localizedMessages: LocalizedValues? = null
    )

    private fun isProjectMatch(project: String, filter: ProjectFilter): Boolean = when (filter) {
        is String -> project.toLowerCase() == filter.toLowerCase()
        is List<*> -> project.toLowerCase() in filter.map { (it as String).toLowerCase() }
        else -> false
    }

    private fun localizeValue(value: String, localizedValues: LocalizedValues?, lang: String) =
        if (lang != "en" && localizedValues != null) {
            localizedValues[lang] ?: value
        } else {
            value
        }

    private fun resolveVariables(message: String, project: String, lang: String): String {
        return variables.entries.fold(message) { message, (key, list) ->
            val variable = list.find { isProjectMatch(project, it.project) }
            message.replace("%$key%", localizeValue(variable?.value ?: "", variable?.localizedValues, lang))
        }
    }

    private fun resolvePlaceholder(message: String, filledText: String? = null): String {
        return message.replace("%s%", filledText ?: "")
    }

    private fun appendOriginalMessageIfLocalized(
        message: String,
        project: String,
        key: String,
        filledText: String? = null,
        isLocalized: Boolean
    ) = if (isLocalized) {
        "$message\n${getSingleMessage(project, key, filledText).fold({ "" }, { it })}"
    } else {
        message
    }

    fun getSingleMessage(
        project: String,
        key: String,
        filledText: String? = null,
        lang: String = "en"
    ): Either<Error, String> {
        return messages[key]?.find { isProjectMatch(project, it.project) }
            .rightIfNotNull { Error("Failed to find message for key $key under project $project") }
            .map { localizeValue(it.message, it.localizedMessages, lang) }
            .map { resolvePlaceholder(it, filledText) }
            .map { resolveVariables(it, project, lang) }
    }

    fun getMessage(
        project: String,
        keys: List<String>,
        filledTexts: List<String?> = emptyList<String?>(),
        lang: String = "en"
    ): String {
        val target = keys.mapIndexed { i, key -> getSingleMessage(project, key, filledTexts.getOrNull(i), lang) }
            .map { either -> either.fold({ "" }, { it }) }
            .joinToString("\n")
        return if (lang == "en") {
            target
        } else {
            val origin = getMessage(project, keys, filledTexts, "en")
            if (origin == target) {
                target
            } else {
                "$target\n----\n$origin"
            }
        }
    }

    fun serialize() = Klaxon().toJsonString(this)

    companion object {
        private const val url =
            "https://raw.githubusercontent.com/mojira/helper-messages/gh-pages/assets/js/messages.json"

        fun deserialize(json: String) = Klaxon().parse<HelperMessages>(json)

        fun fetch() = with(URL(url).openConnection() as URLConnection) {
            deserialize(content as String).rightIfNotNull { Error("Couldn't download or deserialize helper messages") }
        }
    }
}
