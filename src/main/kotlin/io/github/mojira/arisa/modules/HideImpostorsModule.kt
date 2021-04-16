package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.extensions.sequence.monad.map
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import java.time.Instant
import java.time.temporal.ChronoUnit

class HideImpostorsModule : Module() {
    override fun execute(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val restrictImpostorComments = comments
                .asSequence()
                .filter(::commentIsRecent)
                .filter(::userContainsBrackets)
                .filter(::isNotStaffRestricted)
                .filter(::userIsNotVolunteer)
                .map { editedComments.add(it.copy(visibilityType = "group", visibilityValue = "staff")) }
                .toList()

            assertNotEmpty(restrictImpostorComments).bind()
        }
    }

    private fun commentIsRecent(comment: Comment) = comment
        .updated
        ?.plus(1, ChronoUnit.DAYS)
        ?.isAfter(Instant.now()) ?: false

    private fun userContainsBrackets(comment: Comment) = with(comment.author?.displayName) {
        this != null && matches("""\[(?:\p{L}|\p{N}|\s)+]\s.+""".toRegex())
    }

    private fun userIsNotVolunteer(comment: Comment) =
        !(comment.author?.groups.orEmpty().any { it == "helper" || it == "global-moderators" || it == "staff" } ?: false)

    private fun isNotStaffRestricted(comment: Comment) =
        comment.visibilityType != "group" || comment.visibilityValue != "staff"
}
