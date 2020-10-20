package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class AttachmentModule(
    private val excludedExtensions: List<String>,
    private val attachmentRemovedMessage: String
) : Module {

    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val functions = attachments
                .filter { endsWithExcludedExtensions(it.name) }
                .map { it.remove }
            assertNotEmpty(functions).bind()
            functions.forEach { it.invoke() }
            addComment(CommentOptions(attachmentRemovedMessage))
        }
    }

    private fun endsWithExcludedExtensions(name: String) =
        excludedExtensions.any { name.endsWith(it) }
}
