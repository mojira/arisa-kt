package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.partially1
import io.github.mojira.arisa.domain.Comment
import java.time.Instant
import java.time.temporal.ChronoUnit

class HideImpostorsModule : Module<HideImpostorsModule.Request> {
    data class Request(val comments: List<Comment>)

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val restrictImpostorComments = comments
                .filter(::commentIsRecent)
                .filter(::userContainsBrackets)
                .filter(::isNotStaffRestricted)
                .filter(::userIsNotVolunteer)
                .map { it.restrict.partially1(it.body) }

            assertNotEmpty(restrictImpostorComments).bind()
            tryRunAll(restrictImpostorComments).bind()
        }
    }

    private fun commentIsRecent(comment: Comment) = comment
        .updated
        .plus(1, ChronoUnit.DAYS)
        .isAfter(Instant.now())

    private fun userContainsBrackets(comment: Comment) = with(comment.author.displayName) {
        matches("""\[(?:\p{L}|\p{N}|\s)+\]\s.+""".toRegex())
    }

    private fun userIsNotVolunteer(comment: Comment) =
        !(comment.getAuthorGroups()?.any { it == "helper" || it == "global-moderators" || it == "staff" } ?: false)

    private fun isNotStaffRestricted(comment: Comment) =
        comment.visibilityType != "group" || comment.visibilityValue != "staff"
}
