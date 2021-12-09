package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.partially1
import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.jira.sanitizeCommentArg
import org.slf4j.LoggerFactory
import java.time.Instant

private val log = LoggerFactory.getLogger("AttachmentModule")

class AttachmentModule(
    private val extensionBlackList: List<String>,
    private val attachmentRemovedMessage: String
) : Module {

    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val endsWithBlacklistedExtensionAdapter = ::endsWithBlacklistedExtensions.partially1(extensionBlackList)
            val attachmentsToDelete = attachments
                .filter { endsWithBlacklistedExtensionAdapter(it.name) }
            assertNotEmpty(attachmentsToDelete).bind()
            val commentInfo = attachmentsToDelete.getCommentInfo()

            val attachmentsString = attachmentsToDelete.joinToString(separator = ", ", transform = Attachment::id)
            log.info("Deleting attachments of issue $key because they have forbidden extensions: $attachmentsString")
            attachmentsToDelete
                .forEach { it.remove() }

            addComment(CommentOptions(attachmentRemovedMessage))
            addRawRestrictedComment("Removed attachments:\n$commentInfo", "helper")
        }
    }

    private fun List<Attachment>.getCommentInfo() = this.joinToString(separator = "\n") {
        val uploaderName = it.uploader!!.name?.let(::sanitizeCommentArg)
        val attachmentName = sanitizeCommentArg(it.name)
        "- [~$uploaderName]: $attachmentName"
    }

    private fun endsWithBlacklistedExtensions(extensionBlackList: List<String>, name: String) =
        extensionBlackList.any { name.endsWith(it) }
}
