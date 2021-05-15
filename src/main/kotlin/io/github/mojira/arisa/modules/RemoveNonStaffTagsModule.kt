package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.partially1
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class RemoveNonStaffTagsModule(private val removalReason: String, private val removePrefix: String) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val updateMeqsComments = comments
                .filter(::hasMeqsTag)
                .filter(::isNotStaffRestricted)
                .map { it.restrict.partially1(removeTags(it.body!!)) }
            val updatePrefixedComments = comments
                .filter(::hasPrefixedTag)
                .filter(::isNotVolunteerRestricted)
                .map { it.restrict.partially1(removeTags(it.body!!)) }

            assertEither(
                assertNotEmpty(updateMeqsComments),
                assertNotEmpty(updatePrefixedComments)
            ).bind()

            if (updateMeqsComments.isNotEmpty()) {
                updateMeqsComments.forEach { it.invoke() }
            } else {
                updatePrefixedComments.forEach { it.invoke() }
            }
        }
    }

    private fun hasMeqsTag(comment: Comment) =
        comment.body?.contains("""MEQS_[A-Z_]+""".toRegex()) ?: false

    private fun hasPrefixedTag(comment: Comment) =
        comment.body?.contains("""${removePrefix}_[A-Z_]+""".toRegex()) ?: false

    private fun isNotVolunteerRestricted(comment: Comment) =
        comment.visibilityType != "group" ||
                !listOf("staff", "global-moderators", "helper").contains(comment.visibilityValue)

    private fun isNotStaffRestricted(comment: Comment) =
        comment.visibilityType != "group" || !listOf("staff", "global-moderators").contains(comment.visibilityValue)

    private fun removeTags(comment: String): String {
        val regex = """MEQS(_[A-Z_]+)||$removePrefix(_[A-Z_]+)""".toRegex()
        return regex.replace(comment) {
            "${it.groupValues[0]}_ARISA_REMOVED${it.groupValues[1]} Removal Reason: $removalReason"
        }
    }
}
