package io.github.mojira.arisa.modules

import arrow.core.Either
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

    override fun invoke(request: PiracyModuleRequest): Either<ModuleError, ModuleResponse> {
        if (request.description.isNullOrEmpty() && request.environment.isNullOrEmpty() && request.summary.isNullOrEmpty()) {
            return OperationNotNeededModuleResponse.left()
        }
        if (piracySignatures.any {
                request.description?.contains(it) == true ||
                    request.environment?.contains(it) == true ||
                    request.summary?.contains(it) == true
            }) {
            addPiracyComment()
            resolveAsInvalid()
            return ModuleResponse.right()
        }
        return OperationNotNeededModuleResponse.left()
    }
}
