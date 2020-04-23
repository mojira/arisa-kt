package io.github.mojira.arisa.infrastructure

import arrow.core.Either
import arrow.core.rightIfNotNull
import com.beust.klaxon.Klaxon

typealias ProjectFilter = Any

data class HelperMessages(
    val variables: Map<String, List<Variable>>,
    val messages: Map<String, List<Message>>
) {
    data class Variable(val project: ProjectFilter, val value: String)
    data class Message(val project: ProjectFilter, val name: String, val message: String, val fillname: List<String>)

    private fun isProjectMatch(project: String, filter: ProjectFilter): Boolean = when(filter) {
        is String -> project.toLowerCase() == filter.toLowerCase()
        is List<*> -> project.toLowerCase() in filter.map { (it as String).toLowerCase() }
        else -> false
    }

    private fun resolveVariables(message: String, project: String): String {
        return variables.entries.fold(message) { message, (key, list) ->
            val value = list.find { isProjectMatch(project, it.project) }?.value
            message.replace("%$key%", value ?: "")
        }
    }

    private fun resolvePlaceholder(message: String, filledText: String? = null): String {
        return message.replace("%s%", filledText ?: "")
    }

    fun getMessage(key: String, project: String, filledText: String? = null): Either<Error, String> =
        messages[key]?.find { isProjectMatch(project, it.project) }
            .rightIfNotNull { Error("Failed to find message for key $key under project $project") }
            .map { it.message }
            .map { resolveVariables(it, project) }
            .map { resolvePlaceholder(it, filledText) }

    companion object {
        fun deserialize(json: String) = Klaxon().parse<HelperMessages>(json)
    }
}
