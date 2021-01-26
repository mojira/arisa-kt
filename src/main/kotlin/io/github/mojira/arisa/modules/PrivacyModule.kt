package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class PrivacyModule(
    private val message: String,
    private val commentNote: String
) : Module {
    private val patterns: List<Regex> = listOf(
        """\(Session ID is token:""".toRegex(),
        """[a-zA-Z0-9.-_]+@[a-zA-Z0-9.-_]+\.[a-zA-Z0-9.-_]{2,}""".toRegex(),
        """--accessToken ey""".toRegex()
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

            changeLog
                .filter { it.created.isAfter(lastRun) }
                .filter { it.changedFromString == null }
                .forEach { string += "${it.changedToString} " }

            val doesStringMatchPatterns = string.matches(patterns)

            val restrictCommentFunctions = comments
                .asSequence()
                .filter { it.created.isAfter(lastRun) }
                .filter { it.visibilityType == null }
                .filter { it.body?.matches(patterns) ?: false }
                .filterNot { it.getAuthorGroups()?.any { it == "helper" ||
                    it == "global-moderators" || it == "staff" } ?: false }
                .map { { it.restrict("${it.body}$commentNote") } }
                .toList()

            assertEither(
                assertTrue(doesStringMatchPatterns),
                assertNotEmpty(restrictCommentFunctions)
            ).bind()

            if (doesStringMatchPatterns) {
                setPrivate()
                addComment(CommentOptions(message))
            }

            restrictCommentFunctions.forEach { it.invoke() }
        }
    }

    private fun String.matches(patterns: List<Regex>) = patterns.any { it.containsMatchIn(this) }
}
