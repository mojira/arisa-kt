package io.github.mojira.arisa.infrastructure

import arrow.core.Either
import arrow.core.left
import arrow.core.rightIfNotNull
import com.beust.klaxon.Klaxon
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.net.URLConnection

typealias ProjectFilter = Any
typealias LocalizedValues = Map<String, String>

private const val URL =
    "https://raw.githubusercontent.com/mojira/helper-messages/gh-pages/assets/js/messages.json"

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

    /**
     * Get a single message from helper messages.
     * @param project The key of the project where the comment will be sent.
     * @param key The key of the message in helper messages.
     * @param filledText A string which will be used to replace all occurrences of `%s%` in the message.
     * @param lang The language that should be used to localize the message. Defaults to `en`.
     * In case the message hasn't been localized for the specific language, the default text will be used.
     */
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

    /**
     * Get a message combined from different message keys and localized languages that is ready for actual comment.
     * @param project The key of the project where the comment will be sent.
     * @param keys A list of messages keys in helper messages. These messages will be joined with a LF character (`\n`)
     * @param filledTexts A list of texts that will be used to replace all occurrences of `%s%` in the corresponding
     * message specified in `keys` with the same index as the text.
     * @param lang The language that should be used to localize the message. Defaults to `en`.
     * If the message for the specific language has a different value than the default message, the result will contain
     * both the message for the specific language and the original message, joined by a horizontal ruler (`\n----\n`).
     */
    fun getMessage(
        project: String,
        keys: List<String>,
        filledTexts: List<String?> = emptyList(),
        lang: String = "en"
    ): String {
        val target = keys
            .mapIndexed { i, key -> getSingleMessage(project, key, filledTexts.getOrNull(i), lang) }
            .joinToString("\n") { either -> either.fold({ "" }, { it }) }
        return if (lang == "en") {
            target
        } else {
            val origin = getMessage(project, keys, filledTexts, "en")
            if (origin == target) {
                origin
            } else {
                "$target\n----\n$origin"
            }
        }
    }

    fun getMessageWithBotSignature(project: String, key: String, filledText: String? = null, lang: String = "en") =
        getMessage(project, listOf(key, "i-am-a-bot"), listOf(filledText), lang)

    fun serialize() = Klaxon().toJsonString(this)

    private fun isProjectMatch(project: String, filter: ProjectFilter): Boolean = when (filter) {
        is String -> project.equals(filter, ignoreCase = true)
        is List<*> -> project.toLowerCase() in filter.map { (it as String).toLowerCase() }
        else -> false
    }

    private fun localizeValue(value: String, localizedValues: LocalizedValues?, lang: String) =
        if (lang != "en" && localizedValues != null) {
            localizedValues[lang] ?: value
        } else {
            value
        }

    private fun resolveVariables(message: String, project: String, lang: String, limit: Int = 10): String {
        return if (limit == 0) {
            message
        } else {
            variables.entries.fold(message) { msg, (key, list) ->
                val variable = list.find { isProjectMatch(project, it.project) }
                val variableValue = localizeValue(variable?.value ?: "", variable?.localizedValues, lang)
                if (msg.contains("%$key%")) {
                    msg.replace("%$key%", resolveVariables(variableValue, project, lang, limit - 1))
                } else {
                    msg
                }
            }
        }
    }

    private fun resolvePlaceholder(message: String, filledText: String? = null): String {
        return message.replace("%s%", filledText ?: "")
    }
}

private fun fetch() = try {
    with(URL(URL).openConnection() as URLConnection) {
        deserialize(inputStream).rightIfNotNull { Error("Couldn't download or deserialize helper messages") }
    }
} catch (e: IOException) {
    e.left()
}

fun deserialize(json: String) = Klaxon().parse<HelperMessages>(json)

private fun deserialize(stream: InputStream) = Klaxon().parse<HelperMessages>(stream)

fun File.getHelperMessages(old: HelperMessages? = null) = fetch().fold(
    {
        if (this.exists()) {
            deserialize(this.readText())!!
        } else {
            old ?: HelperMessages(emptyMap(), emptyMap())
        }
    },
    {
        this.writeText(it.serialize())
        it
    }
)
