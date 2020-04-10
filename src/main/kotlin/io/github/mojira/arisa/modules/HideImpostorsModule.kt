package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

class HideImpostorsModule : Module<HideImpostorsModule.Request> {
    data class Comment(
        val authorDisplayName: String,
        val authorGroups: List<String>?,
        val updated: Date,
        val visibilityType: String?,
        val visibilityValue: String?,
        val restrict: () -> Either<Throwable, Unit>
    )

    data class Request(val comments: List<Comment>)

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val restrictImpostorComments = comments
                .filter(::commentIsRecent)
                .filter(::userContainsBrackets)
                .filter(::userIsNotVolunteer)
                .filter(::isNotStaffRestricted)
                .map { it.restrict }

            assertNotEmpty(restrictImpostorComments).bind()
            tryRunAll(restrictImpostorComments).bind()
        }
    }

    private fun commentIsRecent(comment: Comment) = comment
        .updated
        .toInstant()
        .plus(1, ChronoUnit.DAYS)
        .isAfter(Instant.now())

    private fun userContainsBrackets(comment: Comment) = with(comment.authorDisplayName) {
        contains("[") && contains("]")
    }

    private fun userIsNotVolunteer(comment: Comment) =
        !(comment.authorGroups?.any { it == "helper" || it == "global-moderator" || it == "staff" } ?: false)

    private fun isNotStaffRestricted(comment: Comment) =
        comment.visibilityType != "group" || comment.visibilityValue != "staff"
}
