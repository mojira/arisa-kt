package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import java.time.Instant

const val MINIMUM_PERCENTAGE = 0.7

class LanguageModule(
    private val allowedLanguages: List<String> = listOf("en"),
    val lengthThreshold: Int = 0
) : Module<LanguageModule.Request> {

    data class Request(
        val created: Instant,
        val lastRun: Instant,
        val summary: String?,
        val description: String?,
        val securityLevel: String?,
        val privateLevel: String,
        val getLanguage: (String) -> Either<Any, Map<String, Double>>,
        val resolveAsInvalid: () -> Either<Throwable, Unit>,
        val addLanguageComment: (language: String) -> Either<Throwable, Unit>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            assertAfter(created, lastRun).bind()
            assertIsPublic(securityLevel, privateLevel).bind()

            val combinedText = combineSummaryAndDescription(summary, description)

            assertExceedLengthThreshold(combinedText).bind()

            val detectedLanguage = getDetectedLanguage(getLanguage, combinedText)

            assertNotNull(detectedLanguage).bind()

            assertLanguageIsNotAllowed(allowedLanguages, detectedLanguage!!).bind()

            addLanguageComment(detectedLanguage).toFailedModuleEither().bind()
            resolveAsInvalid().toFailedModuleEither().bind()
        }
    }

    private fun combineSummaryAndDescription(summary: String?, description: String?): String {
        val trimmedSummary = (summary ?: "").trim()
        val trimmedDescription = (description ?: "").trim()
        return when {
            trimmedDescription.contains(trimmedSummary, ignoreCase = true) -> trimmedDescription.completeDot()
            trimmedSummary.contains(trimmedDescription, ignoreCase = true) -> trimmedSummary.completeDot()
            else -> "${trimmedSummary.completeDot()} ${trimmedDescription.completeDot()}"
        }
    }

    private fun String.completeDot() = if (this.endsWith(".")) {
        this
    } else {
        "$this."
    }

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
