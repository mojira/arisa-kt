package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class PrivacyModule(
    private val message: String
) : Module {
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

            attachments
                .asSequence()
                .filter { it.created.isAfter(lastRun) }
                .filter { it.mimeType.startsWith("text/") }
                .forEach { string += "${String(it.getContent())} " }

            val stringMatchesPatterns = string.matches(patterns)

            val restrictCommentFunctions = comments
                .asSequence()
                .filter { it.created.isAfter(lastRun) }
                .filter { it.visibilityType == null }
                .filter { it.body?.matches(patterns) ?: false }
                .map { { it.restrict("${it.body}\n----\n[~arisabot]: Restricted by PrivacyModule") } }
                .toList()

            assertEither(
                assertTrue(stringMatchesPatterns),
                assertNotEmpty(restrictCommentFunctions)
            ).bind()

            if (stringMatchesPatterns) {
                setPrivate()
                addComment(CommentOptions(message))
            }

            restrictCommentFunctions.forEach { it.invoke() }
        }
    }

    private fun String.matches(patterns: List<Regex>) = patterns.any { it.containsMatchIn(this) }
}
