package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.partially1

class UpdateLinkedModule : Module<UpdateLinkedModule.Request> {
    data class Request(
        val parentReporter: String,
        val links: List<IssueLink>,
        val linkedField: Double?,
        val setLinks: (Double) -> Either<Throwable, Unit>
    )

    data class IssueLink(
        val reporter: String,
        val type: String
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val duplicates = links
                .filter(::isDuplicate)
                .filter(::notSameReporter.partially1(parentReporter))
                .size.toDouble()
            assertNotEquals(duplicates, linkedField ?: 0.0).bind()

            setLinks(duplicates).toFailedModuleEither().bind()
        }
    }

    private fun isDuplicate(link: IssueLink) =
        link.type == "Duplicate"

    private fun notSameReporter(parentReporter: String, link: IssueLink) =
        parentReporter != link.reporter
}
