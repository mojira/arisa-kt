package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class PrivacyModule(
    private val message: String,
    private val commentNote: String,
    private val allowedEmailsRegex: List<Regex>,
    private val sensitiveFileNames: List<String>
) : Module {
    private val patterns: List<Regex> = listOf(
        """.*\(Session ID is token:.*""".toRegex(),
        """.*--accessToken ey.*""".toRegex(),
        """.*(?<![^\s])(?=[^\s]*[A-Z])(?=[^\s]*[0-9])[A-Z0-9]{17}(?![^\s]).*""".toRegex(),
        // At the moment braintree transaction IDs seem to have 8 chars, but to be future-proof
        // match if there are more chars as well
        """.*\bbraintree:[a-f0-9]{6,12}\b.*""".toRegex(),
        """.*\b([A-Za-z0-9]{4}-){3}[A-Za-z0-9]{4}\b.*""".toRegex()
    )

    private val emailRegex = "(?<!\\[~)\\b[a-zA-Z0-9.\\-_]+@[a-zA-Z.\\-_]+\\.[a-zA-Z.\\-]{2,15}\\b".toRegex()

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
                .filter { it.field != "Attachment" }
                .filter { it.changedFromString == null }
                .forEach { string += "${it.changedToString} " }

            val doesStringMatchPatterns = string.matches(patterns)
            val doesEmailMatches = matchesEmail(string)

            val doesAttachmentNameMatch = attachments
                .asSequence()
                .map(Attachment::name)
                .any(sensitiveFileNames::contains)

            val restrictCommentFunctions = comments
                .asSequence()
                .filter { it.created.isAfter(lastRun) }
                .filter { it.visibilityType == null }
                .filter { it.body?.matches(patterns) ?: false || matchesEmail(it.body ?: "") }
                .filterNot {
                    it.getAuthorGroups()?.any { group ->
                        listOf("helper", "global-moderators", "staff").contains(group)
                    } ?: false
                }
                .map { { it.restrict("${it.body}$commentNote") } }
                .toList()

            assertEither(
                assertTrue(doesStringMatchPatterns),
                assertTrue(doesEmailMatches),
                assertTrue(doesAttachmentNameMatch),
                assertNotEmpty(restrictCommentFunctions)
            ).bind()

            if (doesStringMatchPatterns || doesEmailMatches || doesAttachmentNameMatch) {
                setPrivate()
                addComment(CommentOptions(message))
            }

            restrictCommentFunctions.forEach { it.invoke() }
        }
    }

    private fun matchesEmail(string: String): Boolean {
        return emailRegex
            .findAll(string)
            .filterNot { email -> allowedEmailsRegex.any { regex -> regex.matches(email.value) } }
            .any()
    }

    private fun String.matches(patterns: List<Regex>) = patterns.any { it.containsMatchIn(this) }
}
