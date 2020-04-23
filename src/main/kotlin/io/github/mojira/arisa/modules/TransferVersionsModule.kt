package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.syntax.function.partially1
import arrow.syntax.function.partially2

class TransferVersionsModule : Module<TransferVersionsModule.Request> {
    data class LinkedIssue(
        val versions: List<String>,
        val addVersion: (String) -> Either<Throwable, Unit>
    )

    data class Link(
        val type: String,
        val outwards: Boolean,
        val getLinkedIssue: () -> Either<Throwable, LinkedIssue>
    )

    data class Request(
        val links: List<Link>,
        val versions: List<String>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val parentLinks = links.filter(::isDuplicatesLink)
            assertGreaterThan(parentLinks.size, 0).bind()
            val parents = parentLinks
                .map { it.getLinkedIssue() }

            val addVersions = versions.flatMap(::applyParents.partially2(parents)).filterNotNull()
            assertNotEmpty(addVersions).bind()

            tryRunAll(addVersions).bind()
        }
    }

    private fun isDuplicatesLink(link: Link) =
        link.type.toLowerCase() == "duplicate" && link.outwards

    private fun applyParents(version: String, parents: List<Either<Throwable, LinkedIssue>>) =
        parents.map { parentEither ->
            parentEither.fold({ throwable ->
                { throwable.left() }
            }, { parent ->
                if (version !in parent.versions)
                    parent.addVersion.partially1(version)
                else
                    null
            })
        }
}
