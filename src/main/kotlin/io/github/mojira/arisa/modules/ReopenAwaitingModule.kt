package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import net.rcarz.jiraclient.Comment
import net.rcarz.jiraclient.Issue
import net.rcarz.jiraclient.Resolution
import java.util.Date

data class ReopenAwaitingModuleRequest(
    val resolution: Resolution?,
    val created: Date,
    val updated: Date,
    val comments: List<Comment>,
    val issue: Issue
)

class ReopenAwaitingModule(val reopen: (Issue) -> Either<Throwable, Unit>) : Module<ReopenAwaitingModuleRequest> {
    override fun invoke(request: ReopenAwaitingModuleRequest): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            assertResolutionIs(resolution, "Awaiting Response").bind()
            assertCommentsNotEmpty(comments).bind()
            assertCreationIsNotRecent(updated.time, created.time).bind()
            val lastComment = comments.last()
            assertUpdateWasNotCausedByEditingComment(
                updated.time, lastComment.updatedDate.time, lastComment.createdDate.time
            ).bind()
            reopen(issue).toFailedModuleEither().bind()
        }
    }

    fun assertResolutionIs(resolution: Resolution?, name: String) = if (resolution == null || resolution.name != name) {
        OperationNotNeededModuleResponse.left()
    } else {
        Unit.right()
    }

    fun assertCreationIsNotRecent(updated: Long, created: Long) = if ((updated - created) < 2000) {
        OperationNotNeededModuleResponse.left()
    } else {
        Unit.right()
    }

    fun assertUpdateWasNotCausedByEditingComment(updated: Long, commentUpdated: Long, commentCreated: Long) =
        if (updated - commentUpdated < 2000 && (commentUpdated - commentCreated) > 2000) {
            OperationNotNeededModuleResponse.left()
        } else {
            Unit.right()
        }

    fun assertCommentsNotEmpty(comments: List<Comment>) = if (comments.isEmpty()) {
        OperationNotNeededModuleResponse.left()
    } else {
        Unit.right()
    }
}
