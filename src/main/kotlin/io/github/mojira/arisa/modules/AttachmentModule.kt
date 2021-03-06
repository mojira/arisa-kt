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
            val functions = attachments
                .filter { endsWithBlacklistedExtensionAdapter(it.name) }
            assertNotEmpty(functions).bind()
            val usernames = functions.getUsernames()
            val attachmentNames = functions.getAttachmentNames()
            functions
                .map { it.remove }
                .forEach { it.invoke() }
            addComment(attachmentRemovedMessage)
            addRawRestrictedComment("Attachment Details:\nFilename: $attachmentNames\nUploader: $usernames", "helper")
        }
    }

    private fun List<Attachment>.getUsernames() = this
        .map { it.uploader!!.name }
        .run {
            when (size) {
                1 -> get(0)
                2 -> "${get(0)}* and *${get(1)}"
                else -> "${subList(0, lastIndex).joinToString("*, *")}*, and *${last()}"
            }
        }

    private fun List<Attachment>.getAttachmentNames() = this
        .map { it.name }
        .run {
            when (size) {
                1 -> get(0)
                2 -> "${get(0)} and ${get(1)}"
                else -> "${subList(0, lastIndex).joinToString(", ")}, and ${last()}"
            }
        }

    private fun endsWithBlacklistedExtensions(extensionBlackList: List<String>, name: String) =
        extensionBlackList.any { name.endsWith(it) }
}
