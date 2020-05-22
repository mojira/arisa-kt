package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class PrivacyModule : Module {
    private val patterns: List<Regex> = listOf(
        """\(Session ID is token:""".toRegex(),
        """\S+@\S+\.\S{2,}""".toRegex()
    )

    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertNull(securityLevel).bind()

            var string = ""

            if (created.isAfter(lastRun)) {
                string += "$summary $environment $description "
            }

            comments
                .asSequence()
                .filter { it.created.isAfter(lastRun) }
                .filter { it.visibilityType == null }
                .forEach { string += "${it.body} " }

            attachments
                .asSequence()
                .filter { it.created.isAfter(lastRun) }
                .filter { it.mimeType.startsWith("text/") }
                .forEach { string += "${String(it.getContent())} " }

            assertMatchesPatterns(string, patterns).bind()

            setPrivate().toFailedModuleEither().bind()
        }
    }

    private fun assertMatchesPatterns(string: String, patterns: List<Regex>) = when {
        patterns.any { it.containsMatchIn(string) } -> Unit.right()
        else -> OperationNotNeededModuleResponse.left()
    }
}
