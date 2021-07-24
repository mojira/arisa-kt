package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import com.urielsalis.mccrashlib.deobfuscator.getSafeChildPath
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.jira.sanitizeCommentArg
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.time.Instant

private val log = LoggerFactory.getLogger("PrivacyModule")

class PrivacyModule(
    private val message: String,
    private val commentNote: String,
    private val allowedEmailsRegex: List<Regex>,
    private val attachmentRedactor: AttachmentRedactor,
    private val sensitiveFileNames: List<String>
) : Module {
    private val patterns: List<Regex> = listOf(
        """\(Session ID is token:""".toRegex(),
        """--accessToken ey""".toRegex(),
        """(?<![^\s])(?=[^\s]*[A-Z])(?=[^\s]*[0-9])[A-Z0-9]{17}(?![^\s])""".toRegex(),
        // At the moment braintree transaction IDs seem to have 8 chars, but to be future-proof
        // match if there are more chars as well
        """\bbraintree:[a-f0-9]{6,12}\b""".toRegex()
    )

    private val emailRegex = "(?<!\\[~)\\b[a-zA-Z0-9.\\-_]+@[a-zA-Z.\\-_]+\\.[a-zA-Z.\\-]{2,15}\\b".toRegex()

    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertNull(securityLevel).bind()

            val (attachmentContainsSensitiveData, attachmentsToRedact) = checkAttachments(lastRun)
            val issueContainsSensitiveData = attachmentContainsSensitiveData || containsIssueSensitiveData(lastRun)

            val restrictCommentActions = getRestrictCommentActions(lastRun)

            assertEither(
                assertNotEmpty(attachmentsToRedact),
                assertTrue(issueContainsSensitiveData),
                assertNotEmpty(restrictCommentActions)
            ).bind()

            // Always try to redact attachments, even if issue would be made private anyways
            // So in case issue was made private erroneously it can easily be made public
            val redactedAll = redactAttachments(issue, attachmentsToRedact)

            if (!redactedAll || issueContainsSensitiveData) {
                setPrivate()
                addComment(CommentOptions(message))
            }

            restrictCommentActions.forEach { it.invoke() }
        }
    }

    private fun containsSensitiveData(string: String): MatchResult? =
        matchesEmail(string) ?: patterns.asSequence().mapNotNull { it.find(string) }.firstOrNull()

    private fun matchesEmail(string: String): MatchResult? {
        return emailRegex
            .findAll(string)
            .filterNot { email -> allowedEmailsRegex.any { regex -> regex.matches(email.value) } }
            .firstOrNull()
    }

    private data class AttachmentsCheckResult(
        /** Whether any of the attachments contains sensitive data which cannot be redacted */
        val attachmentContainsSensitiveData: Boolean,
        val attachmentsToRedact: List<RedactedAttachment>
    )

    private fun Issue.checkAttachments(lastRun: Instant): AttachmentsCheckResult {
        var attachmentContainsSensitiveData: Boolean
        val newAttachments = attachments.filter { it.created.isAfter(lastRun) }

        attachmentContainsSensitiveData = newAttachments.any {
            sensitiveFileNames.contains(it.name)
        }

        val attachmentsToRedact = newAttachments
            .filter { it.hasTextContent() }
            .mapNotNull {
                // Don't redact bot attachments to guard against infinite loop
                // But still check bot attachments for sensitive data, e.g. when deobfuscated crash report
                // contains sensitive data
                val redacted = if (it.uploader?.isBotUser?.invoke() == true) null else attachmentRedactor.redact(it)
                if (redacted == null) {
                    // No redaction necessary / possible; check if attachment contains sensitive data
                    if (!attachmentContainsSensitiveData) {
                        containsSensitiveData(it.getTextContent())?.let { matchResult ->
                            attachmentContainsSensitiveData = true
                            logFoundSensitiveData("in attachment with ID ${it.id}", matchResult)
                        }
                    }
                    return@mapNotNull null
                } else {
                    // Check if attachment content still contains sensitive data after redacting
                    if (containsSensitiveData(redacted.redactedContent) != null) {
                        if (!attachmentContainsSensitiveData) {
                            // Because attachment won't be redacted get match result in original attachment for logging
                            containsSensitiveData(it.getTextContent())?.let { matchResult ->
                                logFoundSensitiveData("in attachment with ID ${it.id}", matchResult)
                            } ?: run {
                                // Redactor might have produced malformed output, or sensitive data regex pattern
                                // is imprecise; marking attachment as containing sensitive data nonetheless
                                log.warn("$key: Sensitive data was detected in redacted content for attachment with " +
                                        "ID ${it.id}, but original attachment content was not detect")
                            }
                        }

                        attachmentContainsSensitiveData = true
                        // Don't redact if attachment content would still contain sensitive data
                        return@mapNotNull null
                    }
                    return@mapNotNull redacted
                }
            }

        return AttachmentsCheckResult(attachmentContainsSensitiveData, attachmentsToRedact)
    }

    @Suppress("ReturnCount")
    private fun Issue.containsIssueSensitiveData(lastRun: Instant): Boolean {
        if (created.isAfter(lastRun)) {
            summary?.let(::containsSensitiveData)?.let {
                logFoundSensitiveData("in summary", it)
                return true
            }
            environment?.let(::containsSensitiveData)?.let {
                logFoundSensitiveData("in environment", it)
                return true
            }
            description?.let(::containsSensitiveData)?.let {
                logFoundSensitiveData("in description", it)
                return true
            }
        }

        return changeLog
            .asSequence()
            .filter { it.created.isAfter(lastRun) }
            .filter { it.field != "Attachment" }
            .filter { it.changedFromString == null }
            .any {
                it.changedToString?.let(::containsSensitiveData)?.let { matchResult ->
                    logFoundSensitiveData("in change log item ${it.entryId}[${it.itemIndex}]", matchResult)
                    return true
                }
                return false
            }
    }

    private fun Issue.getRestrictCommentActions(lastRun: Instant) = comments
        .asSequence()
        .filter { it.created.isAfter(lastRun) }
        .filter { it.visibilityType == null }
        .filterNot {
            it.getAuthorGroups()?.any { group ->
                listOf("helper", "global-moderators", "staff").contains(group)
            } ?: false
        }
        .filter {
            it.body?.let(::containsSensitiveData)?.let { matchResult ->
                logFoundSensitiveData("in comment with ID ${it.id}", matchResult)
                return@filter true
            }
            return@filter false
        }
        .map { { it.restrict("${it.body}$commentNote") } }
        .toList()

    private fun Issue.logFoundSensitiveData(location: String, matchResult: MatchResult) {
        val range = matchResult.range
        // Important: Don't log value (i.e. sensitive data) of match result
        log.info("$key: Found sensitive data $location at ${range.first}-${range.last}")
    }

    private fun Issue.hasAnyAttachmentName(name: String) = attachments.any { it.name == name }

    private fun redactAttachments(issue: Issue, attachments: Collection<RedactedAttachment>): Boolean {
        var redactedAll = true
        attachments
            // Group by uploader in case they uploaded multiple attachments at once
            .groupBy { it.attachment.uploader?.name!! }
            .forEach { (uploader, userAttachments) ->
                val fileNames = mutableSetOf<String>()
                userAttachments.forEach {
                    val attachment = it.attachment
                    val tempDir = Files.createTempDirectory("arisa-redaction-upload").toFile()
                    val fileName = "redacted_${attachment.name}"
                    val filePath = getSafeChildPath(tempDir, fileName)

                    if (filePath == null || issue.hasAnyAttachmentName(fileName) || !fileNames.add(fileName)) {
                        redactedAll = false
                        // Note: Don't log file name to avoid log injection
                        log.warn("Cannot redact attachment with ID ${attachment.id} of issue ${issue.key}; file name " +
                            "is malformed or would clash with other attachment")
                        tempDir.delete()
                    } else {
                        filePath.writeText(it.redactedContent)
                        issue.addAttachment(filePath) {
                            // Once uploaded, delete the temp directory containing the attachment
                            tempDir.deleteRecursively()
                        }
                        // Remove the original attachment
                        log.info("Deleting attachment with ID ${attachment.id} of issue ${issue.key} because it " +
                            "contains sensitive data")
                        attachment.remove()
                    }
                }

                if (fileNames.isNotEmpty()) {
                    // Use postfix line break to prevent bot signature from appearing as part of last list item
                    val fileNamesString = fileNames.joinToString(separator = "", postfix = "\n") {
                        // Use link for attachments
                        "\n- [^${sanitizeCommentArg(it)}]"
                    }
                    val sanitizedUploaderName = sanitizeCommentArg(uploader)
                    // Does not use helper message because message will only be used by bot and helper messages
                    // currently only support one placeholder
                    issue.addRawBotComment(
                        "@[~$sanitizedUploaderName], sensitive data has been removed from your " +
                            "attachment(s) and they have been re-uploaded as:$fileNamesString"
                    )
                }
            }

        return redactedAll
    }
}
