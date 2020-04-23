package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import java.time.Instant
import java.time.temporal.ChronoUnit

class HideImpostorsModule : Module<HideImpostorsModule.Request> {
    data class Comment(
        val authorDisplayName: String,
        val getAuthorGroups: () -> List<String>?,
        val updated: Instant,
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
                .filter(::isNotStaffRestricted)
                .filter(::userIsNotVolunteer)
                .map { it.restrict }

            assertNotEmpty(restrictImpostorComments).bind()
            tryRunAll(restrictImpostorComments).bind()
        }
    }

    private fun commentIsRecent(comment: Comment) = comment
        .updated
        .plus(1, ChronoUnit.DAYS)
        .isAfter(Instant.now())

    private fun userContainsBrackets(comment: Comment) = with(comment.authorDisplayName) {
        matches("""\[(?:\p{L}|\p{N}|\s)+\]\s.+""".toRegex())
    }

    private fun userIsNotVolunteer(comment: Comment) =
        !(comment.getAuthorGroups()?.any { it == "helper" || it == "global-moderators" || it == "staff" } ?: false)

    private fun isNotStaffRestricted(comment: Comment) =
        comment.visibilityType != "group" || comment.visibilityValue != "staff"
}
