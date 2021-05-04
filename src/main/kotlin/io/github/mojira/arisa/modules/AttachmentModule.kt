package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.partially1
import io.github.mojira.arisa.domain.Attachment
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class AttachmentModule(
    private val extensionBlackList: List<String>,
    private val attachmentRemovedMessage: String
) : Module {

    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val endsWithBlacklistedExtensionAdapter = ::endsWithBlacklistedExtensions.partially1(extensionBlackList)
            val functions = attachments
                .filter { endsWithBlacklistedExtensionAdapter(it.name) }
            assertNotEmpty(functions).bind()
            val commentInfo = functions.getCommentInfo()
            functions
                .map { it.remove }
                .forEach { it.invoke() }
            addComment(CommentOptions(attachmentRemovedMessage))
            addRawRestrictedComment("Removed attachments:\n$commentInfo", "helper")
        }
    }

    private fun List<Attachment>.getCommentInfo() = this
        .map { "- [~${it.uploader!!.name}]: ${it.name}" }
            .joinToString { "\n" }

    private fun endsWithBlacklistedExtensions(extensionBlackList: List<String>, name: String) =
        extensionBlackList.any { name.endsWith(it) }
}
