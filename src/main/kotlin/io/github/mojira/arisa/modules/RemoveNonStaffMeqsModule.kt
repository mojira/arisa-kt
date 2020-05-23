package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.partially1
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class RemoveNonStaffMeqsModule(private val removalReason: String) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val updateMeqsComments = comments
                .filter(::hasMeqsTag)
                .filter(::isNotStaffRestricted)
                .map { it.restrict.partially1(removeMeqsTags(it.body!!)) }
            assertNotEmpty(updateMeqsComments).bind()

            tryRunAll(updateMeqsComments).bind()
        }
    }

    private fun hasMeqsTag(comment: Comment) =
        comment.body?.contains("""MEQS_[A-Z_]+""".toRegex()) ?: false

    private fun isNotStaffRestricted(comment: Comment) =
        comment.visibilityType != "group" || comment.visibilityValue != "staff"

    private fun removeMeqsTags(comment: String): String {
        val regex = """MEQS(_[A-Z_]+)""".toRegex()
        return regex.replace(comment) { "MEQS_ARISA_REMOVED${it.groupValues[1]} Removal Reason: $removalReason" }
    }
}
