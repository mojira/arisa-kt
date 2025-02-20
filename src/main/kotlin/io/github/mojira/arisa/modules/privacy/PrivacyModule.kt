package io.github.mojira.arisa.modules.privacy

import arrow.core.Either
import arrow.core.extensions.fx
import com.urielsalis.mccrashlib.deobfuscator.getSafeChildPath
import io.github.mojira.arisa.domain.cloud.CloudAttachment
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.cloud.CloudIssue
import io.github.mojira.arisa.infrastructure.jira.sanitizeCommentArg
import io.github.mojira.arisa.modules.CloudModule
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.assertAny
import io.github.mojira.arisa.modules.assertNotEmpty
import io.github.mojira.arisa.modules.assertNull
import io.github.mojira.arisa.modules.assertTrue
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.time.Instant

private fun Iterable<Regex>.anyMatches(string: String) = any { it.matches(string) }

private val log = LoggerFactory.getLogger("PrivacyModule")

class PrivacyModule(
    private val message: String,
    private val commentNote: String,
    private val allowedEmailRegexes: List<Regex>,
    private val sensitiveTextRegexes: List<Regex>,
    private val attachmentRedactor: AttachmentRedactor,
    private val sensitiveFileNameRegexes: List<Regex>
) : CloudModule {
    // Matches an email address, which is not part of a user mention ([~name])
    private val emailRegex = "(?<!\\[~)\\b[a-zA-Z0-9.\\-_]+@[a-zA-Z.\\-_]+\\.[a-zA-Z.\\-]{2,15}\\b".toRegex()

    override fun invoke(issue: CloudIssue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertNull(securityLevel).bind()

            val (foundNonRedactableSensitiveData, attachmentsToRedact) = checkAttachments(lastRun)
            val issueContainsSensitiveData = foundNonRedactableSensitiveData || containsCloudIssueSensitiveData(lastRun)

            val restrictCommentActions = getRestrictCommentActions(lastRun)

            assertAny(
                assertNotEmpty(attachmentsToRedact),
                assertTrue(issueContainsSensitiveData),
                assertNotEmpty(restrictCommentActions)
            ).bind()

            // Always try to redact attachments, even if issue would be made private anyways
            // So in case issue was made private erroneously, it can easily be made public manually
            val redactingSucceeded = redactAttachments(issue, attachmentsToRedact)

            if (!redactingSucceeded || issueContainsSensitiveData) {
                setPrivate()
                addComment(CommentOptions(message))
            }

            restrictCommentActions.forEach { it.invoke() }
        }
    }

    private fun containsSensitiveData(string: String): TextRangeLocation? =
        containsEmailMatch(string)
            ?: sensitiveTextRegexes.asSequence()
                .mapNotNull { it.find(string) }
                .map { TextRangeLocation.fromMatchResult(string, it) }
                .firstOrNull()

    private fun containsEmailMatch(string: String): TextRangeLocation? {
        return emailRegex
            .findAll(string)
            .filterNot { match -> allowedEmailRegexes.anyMatches(match.value) }
            .map { TextRangeLocation.fromMatchResult(string, it) }
            .firstOrNull()
    }

    private data class AttachmentsCheckResult(
        /** Whether any of the attachments contains sensitive data which cannot be redacted */
        val foundNonRedactableSensitiveData: Boolean,
        val attachmentsToRedact: List<RedactedAttachment>
    )

    private fun CloudIssue.checkAttachments(lastRun: Instant): AttachmentsCheckResult {
        var foundNonRedactableSensitiveData: Boolean
        val newAttachments = attachments.filter { it.created.isAfter(lastRun) }

        foundNonRedactableSensitiveData = newAttachments
            .map(CloudAttachment::filename)
            .any(sensitiveFileNameRegexes::anyMatches)

        val attachmentsToRedact = newAttachments
            .filter { it.hasTextContent() }
            .mapNotNull {
                // Don't redact bot attachments to guard against infinite loop
                // But still check bot attachments for sensitive data, e.g. when deobfuscated crash report
                // contains sensitive data
                val redacted = if (it.uploader?.isBotUser?.invoke() == true) null else attachmentRedactor.redact(it)
                if (redacted == null) {
                    // No redaction necessary / possible; check if attachment contains sensitive data
                    if (!foundNonRedactableSensitiveData) {
                        containsSensitiveData(it.getTextContent())?.let { matchResult ->
                            foundNonRedactableSensitiveData = true
                            logFoundSensitiveData("in attachment with ID ${it.id}", matchResult)
                        }
                    }
                    return@mapNotNull null
                }
                // Check if attachment content still contains sensitive data after redacting
                else if (containsSensitiveData(redacted.redactedContent) != null) {
                    if (!foundNonRedactableSensitiveData) {
                        // Because attachment won't be redacted get match result in original attachment for logging
                        containsSensitiveData(it.getTextContent())?.let { matchResult ->
                            logFoundSensitiveData("in attachment with ID ${it.id}", matchResult)
                        } ?: run {
                            // Redactor might have produced malformed output, or sensitive data regex pattern
                            // is imprecise; marking attachment as containing sensitive data nonetheless
                            log.warn(
                                "$key: Sensitive data was detected in redacted content for attachment with " +
                                    "ID ${it.id}, but original attachment content was not detect"
                            )
                        }
                    }

                    foundNonRedactableSensitiveData = true
                    // Don't redact if attachment content would still contain sensitive data
                    return@mapNotNull null
                } else {
                    return@mapNotNull redacted
                }
            }

        return AttachmentsCheckResult(foundNonRedactableSensitiveData, attachmentsToRedact)
    }

    @Suppress("ReturnCount")
    private fun CloudIssue.containsCloudIssueSensitiveData(lastRun: Instant): Boolean {
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

    private fun CloudIssue.getRestrictCommentActions(lastRun: Instant) = comments
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

    private fun CloudIssue.logFoundSensitiveData(location: String, matchResult: TextRangeLocation) {
        // Important: Don't log value (i.e. sensitive data) of match result
        log.info("$key: Found sensitive data $location at ${matchResult.getLocationDescription()}")
    }

    private fun CloudIssue.hasAnyAttachmentName(name: String) = attachments.any { it.filename == name }

    /**
     * @return true if all provided attachments have been redacted; false if at least one attachment
     *      still contains sensitive data
     */
    private fun redactAttachments(issue: CloudIssue, attachments: Collection<RedactedAttachment>): Boolean {
        var redactedAll = true
        attachments
            // Group by uploader in case they uploaded multiple attachments at once
            .groupBy { it.attachment.uploader?.accountId!! }
            .forEach { (uploaderId, userAttachments) ->
                val fileNames = mutableSetOf<String>()
                userAttachments.forEach {
                    val attachment = it.attachment
                    val tempDir = Files.createTempDirectory("arisa-redaction-upload").toFile()
                    val fileName = "redacted_${attachment.filename}"
                    val filePath = getSafeChildPath(tempDir, fileName)

                    if (filePath == null || issue.hasAnyAttachmentName(fileName) || !fileNames.add(fileName)) {
                        redactedAll = false
                        // Note: Don't log file name to avoid log injection
                        log.warn(
                            "Cannot redact attachment with ID ${attachment.id} of issue ${issue.key}; file name " +
                                "is malformed or would clash with other attachment"
                        )
                        tempDir.delete()
                    } else {
                        log.info(
                            "Redacting attachment with ID ${attachment.id} of issue ${issue.key} because it " +
                                "contains sensitive data"
                        )
                        filePath.writeText(it.redactedContent)
                        issue.addAttachmentFromFile(filePath) {
                            // Once uploaded, delete the temp directory containing the attachment
                            tempDir.deleteRecursively()
                        }
                        // Remove the original attachment
                        attachment.remove()
                    }
                }

                if (fileNames.isNotEmpty()) {
                    // Use postfix line break to prevent bot signature from appearing as part of last list item
                    val fileNamesString = fileNames.joinToString(separator = "", postfix = "\n") {
                        // Use link for attachments
                        "\n- [^${sanitizeCommentArg(it)}]"
                    }
                    // Does not use helper message because message will only be used by bot and helper messages
                    // currently only support one placeholder
                    issue.addRawBotComment(
                        "[~accountid:$uploaderId], sensitive data has been removed from your " +
                            "attachment(s) and they have been re-uploaded as:$fileNamesString"
                    )
                }
            }

        return redactedAll
    }
}
