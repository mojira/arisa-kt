package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right

class LanguageModule(
    val allowedLanguages: List<String> = listOf("en")
) : Module<LanguageModule.Request> {

    data class Request(
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
            assertIsPublic(securityLevel, privateLevel).bind()

            val detectedLanguage = getDetectedLanguage(getLanguage, request.summary, request.description)

            assertNotNull(detectedLanguage).bind()

            assertLanguageIsNotAllowed(allowedLanguages, detectedLanguage!!).bind()

            addLanguageComment(detectedLanguage).toFailedModuleEither().bind()
            resolveAsInvalid().toFailedModuleEither().bind()
        }
    }

    private fun getDetectedLanguage(
        getLanguage: (String) -> Either<Any, Map<String, Double>>,
        summary: String?,
        description: String?
    ): String? {
        val detected = getLanguage("${summary ?: ""} ${description ?: ""}")
        return detected.fold(
            { null },
            {
                it.filter { it.value > 0.7 }.maxBy { it.value }?.key
            }
        )
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
