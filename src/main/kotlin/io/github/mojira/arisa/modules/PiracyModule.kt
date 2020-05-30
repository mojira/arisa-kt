package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.CommentOptions
import java.time.Instant

class PiracyModule(
    private val piracySignatures: List<String>,
    private val message: String
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertContainsSignatures(
                piracySignatures,
                "$description $environment $summary"
            ).bind()
            addComment(CommentOptions(message)).toFailedModuleEither().bind()
            resolveAsInvalid().toFailedModuleEither().bind()
        }
    }

    private fun assertContainsSignatures(piracySignatures: List<String>, matcher: String) = when {
        piracySignatures.any { """\b${Regex.escape(it)}\b""".toRegex().containsMatchIn(matcher) } -> Unit.right()
        else -> OperationNotNeededModuleResponse.left()
    }
}
