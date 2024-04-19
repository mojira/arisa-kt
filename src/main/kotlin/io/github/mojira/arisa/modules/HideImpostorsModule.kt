package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.partially1
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import java.time.Instant
import java.time.temporal.ChronoUnit

class HideImpostorsModule : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val restrictImpostorComments = comments
                .asSequence()
                .filter(::commentIsRecent)
                .filter(::userContainsBrackets)
                .filter(::isNotStaffRestricted)
                .filter(::authorIsNotVolunteer)
                .filter(::updateAuthorIsNotVolunteer)
                .map { it.restrict.partially1(it.body ?: "") }
                .toList()

            assertNotEmpty(restrictImpostorComments).bind()
            restrictImpostorComments.forEach { it.invoke() }
        }
    }

    private fun commentIsRecent(comment: Comment) = comment
        .updated
        .plus(1, ChronoUnit.DAYS)
        .isAfter(Instant.now())

    private fun userContainsBrackets(comment: Comment) = with(comment.author.displayName) {
        this != null && matches("""\[(?:\p{L}|\p{N}|\s)+]\s.+""".toRegex())
    }

    private val staffGroups = setOf("helper", "global-moderators", "staff")

    private fun authorIsNotVolunteer(comment: Comment) =
        comment.getAuthorGroups()?.none { staffGroups.contains(it) } ?: true

    private fun updateAuthorIsNotVolunteer(comment: Comment) =
        comment.getUpdateAuthorGroups()?.none { staffGroups.contains(it) } ?: true

    private fun isNotStaffRestricted(comment: Comment) =
        comment.visibilityType != "group" || comment.visibilityValue != "staff"
}
