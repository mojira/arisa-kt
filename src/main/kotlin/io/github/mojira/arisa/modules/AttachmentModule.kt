package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.partially1
import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class AttachmentModule(
    private val extensionBlackList: List<String>,
    private val attachmentRemovedMessage: String
) : Module() {
    override fun execute(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val endsWithBlacklistedExtensionAdapter = ::endsWithBlacklistedExtensions.partially1(extensionBlackList)
            val blacklistedAttachments = attachments
                .filter { endsWithBlacklistedExtensionAdapter(it.name) }
            assertNotEmpty(blacklistedAttachments).bind()

            val commentInfo = blacklistedAttachments.getCommentInfo()
            removedAttachments.addAll(blacklistedAttachments)
            addComment(attachmentRemovedMessage)
            addRawComment("Removed attachments:\n$commentInfo", "group", "helper")
        }
    }

    private fun List<Attachment>.getCommentInfo() = this
        .map { "- [~${it.uploader!!.name}]: ${it.name}" }
        .joinToString(separator = "\n")

    private fun endsWithBlacklistedExtensions(extensionBlackList: List<String>, name: String) =
        extensionBlackList.any { name.endsWith(it) }
}
