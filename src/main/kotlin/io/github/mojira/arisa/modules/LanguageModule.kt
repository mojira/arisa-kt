package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Issue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant

private const val MINIMUM_PERCENTAGE = 0.7

class LanguageModule(
    private val allowedLanguages: List<String> = listOf("en"),
    private val lengthThreshold: Int = 0,
    private val getLanguage: (String) -> Map<String, Double>
) : Module {
    val log: Logger = LoggerFactory.getLogger("LanguageModule")

    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertAfter(created, lastRun).bind()
            assertNull(securityLevel).bind()

            val combinedText = combineSummaryAndDescription(summary ?: "", description ?: "")
            assertExceedLengthThreshold(combinedText).bind()

            val detectedLanguage = getDetectedLanguage(getLanguage, combinedText)
            assertNotNull(detectedLanguage).bind()
            assertLanguageIsNotAllowed(allowedLanguages, detectedLanguage!!).bind()

            log.info("Detected language for ${issue.key} is $detectedLanguage")

            addNotEnglishComment(detectedLanguage)
            resolveAsInvalid()
        }
    }

    private fun combineSummaryAndDescription(summary: String, description: String): String {
        return when {
            description.contains(summary, ignoreCase = true) -> description.normalizeDescription()
            summary.contains(description, ignoreCase = true) -> summary.normalizeDescription()
            else -> "${summary.normalizeDescription()} ${description.normalizeDescription()}"
        }
    }

    private fun String.normalizeDescription() =
        this
            .stripUrls()
            .trim()
            .completeDot()

    private fun String.completeDot() = if (this.endsWith(".")) {
        this
    } else {
        "$this."
    }

    private fun String.stripUrls() =
        this.replace("""(?:https?://|www\.)[a-zA-Z0-9_-]+(?:\.[a-zA-Z0-9_-]+)+\S*""".toRegex(), "")

    private fun getDetectedLanguage(
        getLanguage: (String) -> Map<String, Double>,
        text: String
    ): String? {
        val detected = getLanguage(text)
        return detected.filter { it.value > MINIMUM_PERCENTAGE }.maxByOrNull { it.value }?.key
    }

    private fun assertExceedLengthThreshold(text: String) = when {
        text.length < lengthThreshold -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }

    private fun assertLanguageIsNotAllowed(allowedLanguages: List<String>, language: String) = when {
        allowedLanguages.any { language == it } -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }
}
