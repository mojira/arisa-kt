package io.github.mojira.arisa.infrastructure

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.core.rightIfNotNull
import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import io.github.mojira.arisa.infrastructure.jira.sanitizeCommentArg
import io.github.mojira.arisa.modules.openHttpGetInputStream
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI

private typealias ProjectFilter = Any
private typealias LocalizedValues = Map<String, String>

private const val URL =
    "https://raw.githubusercontent.com/mojira/helper-messages/gh-pages/assets/js/messages.json"

private val logger = LoggerFactory.getLogger("HelperMessages")

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

@Suppress("TooManyFunctions")
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
            .map { resolvePlaceholder(it, filledText?.let(::sanitizeCommentArg)) }
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

    private const val BOT_SIGNATURE_KEY = "i-am-a-bot"
    fun getMessageWithBotSignature(project: String, key: String, filledText: String? = null, lang: String = "en") =
        getMessage(project, listOf(key, BOT_SIGNATURE_KEY), listOf(filledText), lang)

    fun getMessageWithDupeBotSignature(project: String, key: String, filledText: String? = null, lang: String = "en") =
        getMessage(project, listOf(key, "i-am-a-bot-dupe"), listOf(filledText), lang)

    fun getRawMessageWithBotSignature(rawMessage: String): String {
        // Note: Project does not matter, message is (currently) the same for all projects
        val botSignature = getSingleMessage("MC", BOT_SIGNATURE_KEY).getOrElse { "" }
        return "$rawMessage\n$botSignature"
    }

    fun setHelperMessages(json: String) = data.fromJSON(json)
        ?: throw IOException("Couldn't deserialize helper messages from setHelperMessages()")

    fun updateHelperMessages(file: File) {
        fetchHelperMessages().fold(
            { loadCachedHelperMessages(file, it) },
            { inputStream ->
                inputStream.use {
                    try {
                        data.fromStream(it)
                    } catch (exception: KlaxonException) {
                        loadCachedHelperMessages(file, exception)
                    }
                    file.writeText(data.toJSON())
                }
            }
        )
    }

    private fun fetchHelperMessages() = try {
        openHttpGetInputStream(URI(URL)).right()
    } catch (e: IOException) {
        e.left()
    }

    /**
     * @param fetchException
     *      Exception which caused fetching or deserializing fetched helper messages to fail
     */
    private fun loadCachedHelperMessages(file: File, fetchException: Exception) {
        if (file.exists()) {
            logger.warn("Failed fetching helper messages, reusing cached ones", fetchException)
            try {
                data.fromJSON(file.readText())
            } catch (@Suppress("TooGenericExceptionCaught") exception: Exception) {
                throw IOException("Failed loading cached helper messages", exception)
            }
        } else {
            throw IOException("Fetching helper messages failed and no cached ones exist", fetchException)
        }
    }

    private fun isProjectMatch(project: String, filter: ProjectFilter): Boolean = when (filter) {
        is String -> project.equals(filter, ignoreCase = true)
        is List<*> -> project.lowercase() in filter.map { (it as String).lowercase() }
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
