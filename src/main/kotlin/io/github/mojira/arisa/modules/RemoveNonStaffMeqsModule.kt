package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class RemoveNonStaffMeqsModule(private val removalReason: String) : Module() {
    override fun execute(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val updateMeqsComments = comments
                .filter(::hasMeqsTag)
                .filter(::isNotStaffRestricted)
                .map { it.restrict.partially1(removeMeqsTags(it.body!!)) }
            assertNotEmpty(updateMeqsComments).bind()

            updateMeqsComments.forEach { it.invoke() }
        }
    }

    private fun hasMeqsTag(comment: Comment) =
        comment.body?.contains("""MEQS_[A-Z_]+""".toRegex()) ?: false

    private fun isNotStaffRestricted(comment: Comment) =
        comment.visibilityType != "group" || !listOf("staff", "global-moderators").contains(comment.visibilityValue)

    private fun removeMeqsTags(comment: String): String {
        val regex = """MEQS(_[A-Z_]+)""".toRegex()
        return regex.replace(comment) { "MEQS_ARISA_REMOVED${it.groupValues[1]} Removal Reason: $removalReason" }
    }
}
