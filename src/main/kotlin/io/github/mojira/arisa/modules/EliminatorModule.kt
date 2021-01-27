package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.partially1
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class EliminatorModule(
    private val eliminatedUsernames: List<String>
) : Module {

    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val attachmentFunctions = attachments
                .filter { it.uploader?.name in eliminatedUsernames }
                .map { it::remove }
            val commentFunctions = comments
                .filter { it.author.name in eliminatedUsernames }
                .filter(::isNotStaffRestricted)
                .map { it.restrict.partially1((it.body ?: "") + "\n----\nUser is being eliminated by [~arisabot].") }

            assertEither(
                assertNotEmpty(attachmentFunctions),
                assertNotEmpty(commentFunctions),
                assertAfter(created, lastRun)
            ).bind()

            attachmentFunctions.forEach { it.invoke() }
            commentFunctions.forEach { it.invoke() }
            if (
                created.isAfter(lastRun) &&
                reporter?.name in eliminatedUsernames &&
                securityLevel != project.privateSecurity
            ) {
                setPrivate()
                addRawRestrictedComment("MEQS_KEEP_PRIVATE\nARISA_PLEASE_TRASH_THIS", "staff")
            }
        }
    }

    private fun isNotStaffRestricted(comment: Comment) =
        comment.visibilityType != "group" || comment.visibilityValue != "staff"
}
