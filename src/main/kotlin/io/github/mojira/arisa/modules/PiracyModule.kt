package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right

data class PiracyModuleRequest(
    val environment: String?,
    val summary: String?,
    val description: String?
)

class PiracyModule(
    val resolveAsInvalid: () -> Either<Throwable, Unit>,
    val addPiracyComment: () -> Either<Throwable, Unit>,
    val piracySignatures: List<String>
) : Module<PiracyModuleRequest> {

    override fun invoke(request: PiracyModuleRequest): Either<ModuleError, ModuleResponse> = Either.fx {
        assertContainsSignatures(piracySignatures, request.description + request.environment + request.summary).bind()
        addPiracyComment().toFailedModuleEither().bind()
        resolveAsInvalid().toFailedModuleEither().bind()
    }

    private fun assertContainsSignatures(piracySignatures: List<String>, matcher: String) = when {
        piracySignatures.any { matcher.contains(it) } -> Unit.right()
        else -> OperationNotNeededModuleResponse.left()
    }
}
