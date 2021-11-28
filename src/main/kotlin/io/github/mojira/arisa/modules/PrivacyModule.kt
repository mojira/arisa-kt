package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

private fun Iterable<Regex>.anyMatches(string: String) = any { it.matches(string) }

class PrivacyModule(
    private val message: String,
    private val commentNote: String,
    private val allowedEmailRegexes: List<Regex>,
    private val sensitiveTextRegexes: List<Regex>,
    private val sensitiveFileNameRegexes: List<Regex>
) : Module {
    // Matches an email address, which is not part of a user mention ([~name])
    private val emailRegex = "(?<!\\[~)\\b[a-zA-Z0-9.\\-_]+@[a-zA-Z.\\-_]+\\.[a-zA-Z.\\-]{2,15}\\b".toRegex()

    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertNull(securityLevel).bind()

            var string = ""

            if (created.isAfter(lastRun)) {
                string += "$summary $environment $description "
            }

            val newAttachments = attachments.filter { it.created.isAfter(lastRun) }
            newAttachments
                .asSequence()
                .filter { it.mimeType.startsWith("text/") }
                .forEach { string += "${String(it.getContent())} " }

            changeLog
                .filter { it.created.isAfter(lastRun) }
                .filter { it.field != "Attachment" }
                .filter { it.changedFromString == null }
                .forEach { string += "${it.changedToString} " }

            val doesStringMatchPatterns = string.containsMatch(sensitiveTextRegexes)
            val doesEmailMatches = matchesEmail(string)

            val doesAttachmentNameMatch = newAttachments
                .asSequence()
                .map(Attachment::name)
                .any(sensitiveFileNameRegexes::anyMatches)

            val restrictCommentFunctions = comments
                .asSequence()
                .filter { it.created.isAfter(lastRun) }
                .filter { it.visibilityType == null }
                .filter { it.body?.containsMatch(sensitiveTextRegexes) ?: false || matchesEmail(it.body ?: "") }
                .filterNot {
                    it.getAuthorGroups()?.any { group ->
                        listOf("helper", "global-moderators", "staff").contains(group)
                    } ?: false
                }
                .map { { it.restrict("${it.body}$commentNote") } }
                .toList()

            assertAny(
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
            .map(MatchResult::value)
            .filterNot(allowedEmailRegexes::anyMatches)
            .any()
    }

    private fun String.containsMatch(patterns: List<Regex>) = patterns.any { it.containsMatchIn(this) }
}
