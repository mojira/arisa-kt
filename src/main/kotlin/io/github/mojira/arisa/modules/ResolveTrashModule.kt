package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx

class ResolveTrashModule() : Module<ResolveTrashModule.Request> {
    data class Request(
        val projectKey: String,
        val resolveAsInvalid: () -> Either<Throwable, Unit>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            assertEquals(projectKey, "TRASH").bind()
            resolveAsInvalid().toFailedModuleEither().bind()
        }
    }
}
