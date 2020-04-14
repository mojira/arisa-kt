package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx

class UpdateLinkedModule : Module<UpdateLinkedModule.Request> {
    data class Request(
        val links: List<String>,
        val linkedField: Double?,
        val setLinks: (Double) -> Either<Throwable, Unit>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val duplicates = links.filter(::isDuplicate).size.toDouble()
            assertNotEquals(duplicates, linkedField ?: 0.0).bind()

            setLinks(duplicates).toFailedModuleEither().bind()
        }
    }

    private fun isDuplicate(link: String) =
        link == "Duplicate"
}
