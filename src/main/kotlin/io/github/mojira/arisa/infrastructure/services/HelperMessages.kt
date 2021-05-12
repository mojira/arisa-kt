package io.github.mojira.arisa.infrastructure.services

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
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

data class HelperMessageVariable(
    val project: ProjectFilter,
    val value: String,
    val localizedValues: LocalizedValues? = null
)

data class HelperMessage(
    val project: ProjectFilter,
    val name: String,
    val message: String,
    val fillname: List<String>,
    val localizedMessages: LocalizedValues? = null
)

data class HelperMessageData(
    var variables: Map<String, List<HelperMessageVariable>>,
    var messages: Map<String, List<HelperMessage>>
) {
    fun toJSON() = Klaxon().toJsonString(this)

    /**
     * Replaces the current data with the deserialized data from the given JSON string
     */
    fun fromJSON(json: String) =
        Klaxon().parse<HelperMessageData>(json)
            ?.also {
                variables = it.variables
                messages = it.messages
            }

    /**
     * Replaces the current data with the deserialized data from the given input stream
     */
    fun fromStream(stream: InputStream) =
        Klaxon().parse<HelperMessageData>(stream)
            ?.also {
                variables = it.variables
                messages = it.messages
            }
}

object HelperMessageService {
    var data = HelperMessageData(mapOf(), mapOf())

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
        return data.messages[key]?.find { isProjectMatch(project, it.project) }
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
    ): Either<Error, String> {
        val message = keys
            .mapIndexed { i, key -> getSingleMessage(project, key, filledTexts.getOrNull(i), lang) }
            // Convert the list of Either to an Either of list.
            .run {
                if (all { it.isRight() })
                    map { (it as Either.Right).b }.right()
                else
                    first { it.isLeft() } as Either.Left
            }
            .map { it.joinToString("\n") }
        return if (lang == "en") {
            message
        } else {
            Either.fx {
                val origin = getMessage(project, keys, filledTexts, "en").bind()
                val translation = message.bind()
                if (origin == translation) {
                    origin
                } else {
                    "$translation\n----\n$origin"
                }
            }
        }
    }

    fun getMessageWithBotSignature(project: String, key: String, filledText: String? = null, lang: String = "en") =
        getMessage(project, listOf(key, "i-am-a-bot"), listOf(filledText), lang)

    fun setHelperMessages(json: String) = data.fromJSON(json).rightIfNotNull {
        Error("Couldn't deserialize helper messages from setHelperMessages()")
    }

    fun updateHelperMessages(file: File) = fetchHelperMessages().fold(
        {
            if (file.exists()) {
                data.fromJSON(file.readText()).rightIfNotNull {
                    Error("Couldn't deserialize saved helper messages")
                }
            } else {
                it.left()
            }
        },
        { inputStream ->
            file.writeText(data.toJSON())
            data.fromStream(inputStream).rightIfNotNull {
                Error("Couldn't deserialize downloaded helper messages")
            }
        }
    )

    private fun fetchHelperMessages() = try {
        with(URL(URL).openConnection() as URLConnection) {
            inputStream.rightIfNotNull {
                Error("Couldn't download helper messages")
            }
        }
    } catch (e: IOException) {
        e.left()
    }

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
            data.variables.entries.fold(message) { msg, (key, list) ->
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
