package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

const val MINIMUM_PERCENTAGE = 0.7

class LanguageModule(
    private val allowedLanguages: List<String> = listOf("en"),
    private val lengthThreshold: Int = 0,
    private val getLanguage: (String) -> Either<Any, Map<String, Double>>
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertAfter(created, lastRun).bind()
            assertIsPublic(securityLevel, project.privateSecurity).bind()

            val combinedText = combineSummaryAndDescription(summary, description)
            assertExceedLengthThreshold(combinedText).bind()

            val detectedLanguage = getDetectedLanguage(getLanguage, combinedText)
            assertNotNull(detectedLanguage).bind()
            assertLanguageIsNotAllowed(allowedLanguages, detectedLanguage!!).bind()

            addNotEnglishComment(detectedLanguage)
            resolveAsInvalid()
        }
    }

    private fun combineSummaryAndDescription(summary: String?, description: String?): String {
        val trimmedSummary = (summary ?: "").trim()
        val trimmedDescription = (description ?: "").trim()
        return when {
            trimmedDescription.contains(trimmedSummary, ignoreCase = true) -> trimmedDescription.normalizeDescription()
            trimmedSummary.contains(trimmedDescription, ignoreCase = true) -> trimmedSummary.normalizeDescription()
            else -> "${trimmedSummary.normalizeDescription()} ${trimmedDescription.normalizeDescription()}"
        }
    }

    private fun String.normalizeDescription() =
        this
            .stripUrls()
            .completeDot()

    private fun String.completeDot() = if (this.endsWith(".")) {
        this
    } else {
        "$this."
    }

    private fun String.stripUrls() =
        this.replace("""(?:https?://|www\.)[a-zA-Z0-9_-]+(?:\.[a-zA-Z0-9_-]+)+\S*""".toRegex(), "")

    private fun getDetectedLanguage(
        getLanguage: (String) -> Either<Any, Map<String, Double>>,
        text: String
    ): String? {
        val detected = getLanguage(text)
        return detected.fold(
            { null },
            { languages ->
                languages.filter { it.value > MINIMUM_PERCENTAGE }.maxBy { it.value }?.key
            }
        )
    }

    private fun assertExceedLengthThreshold(text: String) = when {
        text.length < lengthThreshold -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }

    private fun assertIsPublic(securityLevel: String?, privateLevel: String) = when {
        securityLevel == privateLevel -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }

    private fun assertLanguageIsNotAllowed(allowedLanguages: List<String>, language: String) = when {
        allowedLanguages.any { language == it } -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }
}
