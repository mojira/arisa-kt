package io.github.mojira.arisa.infrastructure

import arrow.core.Either
import arrow.core.rightIfNotNull
import com.beust.klaxon.Klaxon
import java.net.URL
import java.net.URLConnection

typealias ProjectFilter = Any
typealias LocaledValue = Map<String, String>

data class HelperMessages(
    val variables: Map<String, List<Variable>>,
    val messages: Map<String, List<Message>>
) {
    data class Variable(
        val project: ProjectFilter,
        val value: String,
        val localedValue: LocaledValue? = null
    )
    data class Message(
        val project: ProjectFilter,
        val name: String,
        val message: String,
        val fillname: List<String>,
        val localedMessage: LocaledValue? = null
    )

    private fun isProjectMatch(project: String, filter: ProjectFilter): Boolean = when (filter) {
        is String -> project.toLowerCase() == filter.toLowerCase()
        is List<*> -> project.toLowerCase() in filter.map { (it as String).toLowerCase() }
        else -> false
    }

    private fun localeValue(value: String, localedValue: LocaledValue?, lang: String) =
        if (lang != "en" && localedValue != null) {
            localedValue[lang] ?: value
        } else {
            value
        }

    private fun resolveVariables(message: String, project: String, lang: String): String {
        return variables.entries.fold(message) { message, (key, list) ->
            val variable = list.find { isProjectMatch(project, it.project) }
            message.replace("%$key%", localeValue(variable?.value ?: "", variable?.localedValue, lang))
        }
    }

    private fun resolvePlaceholder(message: String, filledText: String? = null): String {
        return message.replace("%s%", filledText ?: "")
    }

    fun getMessage(key: String, project: String, filledText: String? = null, lang: String = "en"): Either<Error, String> =
        messages[key]?.find { isProjectMatch(project, it.project) }
            .rightIfNotNull { Error("Failed to find message for key $key under project $project") }
            .map { localeValue(it.message, it.localedMessage, lang) }
            .map { resolveVariables(it, project, lang) }
            .map { resolvePlaceholder(it, filledText) }

    companion object {
        private const val url = "https://raw.githubusercontent.com/mojira/helper-messages/gh-pages/assets/js/messages.json"

        fun deserialize(json: String) = Klaxon().parse<HelperMessages>(json)

        fun download() = with(URL(url).openConnection() as URLConnection) {
            deserialize(content as String).rightIfNotNull { Error("Couldn't download or deserialize helper messages") }
        }
    }
}
