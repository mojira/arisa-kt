package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import net.rcarz.jiraclient.Issue

data class PiracyModuleRequest(
    val issue: Issue,
    val environment: String?,
    val summary: String?,
    val description: String?
)

class PiracyModule(
    val resolveAsInvalid: (Issue) -> Either<Throwable, Unit>,
    val addPiracyComment: (Issue) -> Either<Throwable, Unit>,
    val piracySignatures: List<String>
) : Module<PiracyModuleRequest> {

    override fun invoke(request: PiracyModuleRequest): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            assertContainsSignatures(
                piracySignatures,
                "${description} ${environment} ${summary}"
            ).bind()
            addPiracyComment(issue).toFailedModuleEither().bind()
            resolveAsInvalid(issue).toFailedModuleEither().bind()
        }
    }

    private fun assertContainsSignatures(piracySignatures: List<String>, matcher: String) = when {
        piracySignatures.any { """\b${Regex.escape(it)}\b""".toRegex().containsMatchIn(matcher) } -> Unit.right()
        else -> OperationNotNeededModuleResponse.left()
    }
}
