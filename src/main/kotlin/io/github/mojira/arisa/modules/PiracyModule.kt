package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right

class PiracyModule(private val piracySignatures: List<String>) : Module<PiracyModule.Request> {

    data class Request(
        val environment: String?,
        val summary: String?,
        val description: String?,
        val resolveAsInvalid: () -> Either<Throwable, Unit>,
        val addPiracyComment: () -> Either<Throwable, Unit>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            assertContainsSignatures(
                piracySignatures,
                "$description $environment $summary"
            ).bind()
            addPiracyComment().toFailedModuleEither().bind()
            resolveAsInvalid().toFailedModuleEither().bind()
        }
    }

    private fun assertContainsSignatures(piracySignatures: List<String>, matcher: String) = when {
        piracySignatures.any { """\b${Regex.escape(it)}\b""".toRegex().containsMatchIn(matcher) } -> Unit.right()
        else -> OperationNotNeededModuleResponse.left()
    }
}
