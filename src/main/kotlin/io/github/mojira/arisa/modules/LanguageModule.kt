package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import com.github.pemistahl.lingua.api.Language
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder

class LanguageModule(
    private val allowedLanguages: List<Language> = listOf(
        Language.ENGLISH,
        Language.UNKNOWN
    )
) : Module<LanguageModule.Request> {

    companion object {
        private val detector = LanguageDetectorBuilder.fromLanguages(
            Language.CHINESE,
            Language.ENGLISH,
            Language.FRENCH,
            Language.GERMAN,
            Language.JAPANESE,
            Language.KOREAN,
            Language.POLISH,
            Language.PORTUGUESE,
            Language.RUSSIAN,
            Language.SPANISH
        ).build()
    }

    data class Request(
        val summary: String?,
        val description: String?,
        val resolveAsInvalid: () -> Either<Throwable, Unit>,
        val addLanguageComment: (detectedLanguage: Language) -> Either<Throwable, Unit>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val detectedLanguage = getDetectedLanguage(request.summary, request.description)

            assertLanguageIsNotAllowed(allowedLanguages, detectedLanguage).bind()

            addLanguageComment(detectedLanguage).toFailedModuleEither().bind()
            resolveAsInvalid().toFailedModuleEither().bind()
        }
    }

    private fun getDetectedLanguage(summary: String?, description: String?): Language {
        return detector.detectLanguageOf("${summary ?: ""} ${description ?: ""}")
    }

    private fun assertLanguageIsNotAllowed(allowedLanguages: List<Language>, language: Language) = when {
        allowedLanguages.any { language == it } -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }
}
