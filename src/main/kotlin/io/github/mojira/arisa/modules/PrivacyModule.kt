package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class PrivacyModule(
    private val message: String,
    private val commentNote: String,
    private val allowedEmailsRegex: List<Regex>
) : Module() {
    private val patterns: List<Regex> = listOf(
        """.*\(Session ID is token:.*""".toRegex(),
        """.*--accessToken ey.*""".toRegex(),
        """.*(?<![^\s])(?=[^\s]*[A-Z])(?=[^\s]*[0-9])[A-Z0-9]{17}(?![^\s]).*""".toRegex(),
        """.*\b([A-Z0-9]{4}-){3}[A-Z0-9]{4}\b.*""".toRegex(),
        """.*\bbraintree:[0-9]{6,7}\b.*""".toRegex(),
        """.*\b([A-Z0-9]{5}-){5}[A-Z0-9]{5}\b.*""".toRegex(),
        """.*\b([A-Za-z0-9]{4}-){3}[A-Za-z0-9]{4}\b.*""".toRegex(),
        """.*\b[a-z0-9]{32}\b.*""".toRegex()
    )

    private val emailRegex = "(?<!\\[~)\\b[a-zA-Z0-9.\\-_]+@[a-zA-Z0-9.\\-_]+\\.[a-zA-Z0-9.\\-]{2,15}\\b".toRegex()

    override fun execute(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
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
                .forEach { string += "${String(it.content.get())} " }

            changeLog
                .filter { it.created.isAfter(lastRun) }
                .filter { it.field != "Attachment" }
                .filter { it.changedFromString == null }
                .forEach { string += "${it.changedToString} " }

            val doesStringMatchPatterns = string.matches(patterns)
            val doesEmailMatches = matchesEmail(string)

            val restrictCommentFunctions = comments
                .asSequence()
                .filter { it.created.isAfter(lastRun) }
                .filter { it.visibilityType == null }
                .filter { it.body?.matches(patterns) ?: false || matchesEmail(it.body ?: "") }
                .filterNot {
                    it.author?.groups?.any { group ->
                        listOf("helper", "global-moderators", "staff").contains(group)
                    } ?: false
                }
                .map {
                    {
                        editedComments.add(
                            it.copy(
                                visibilityType = "group",
                                visibilityValue = "staff",
                                body = "${it.body}$commentNote"
                            )
                        )
                    }
                }
                .toList()

            assertEither(
                assertTrue(doesStringMatchPatterns),
                assertTrue(doesEmailMatches),
                assertNotEmpty(restrictCommentFunctions)
            ).bind()

            if (doesStringMatchPatterns || doesEmailMatches) {
                securityLevel = project.privateSecurity
                addComment(message)
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
