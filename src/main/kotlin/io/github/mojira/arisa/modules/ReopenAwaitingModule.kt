package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import java.time.Instant

class ReopenAwaitingModule : Module<ReopenAwaitingModule.Request> {
    data class Comment(
        val updated: Long,
        val created: Long
    )

    data class ChangeLogItem(
        val created: Long,
        val changedTo: String?
    )

    data class Request(
        val resolution: String?,
        val created: Instant,
        val updated: Instant,
        val comments: List<Comment>,
        val changeLog: List<ChangeLogItem>,
        val reopen: () -> Either<Throwable, Unit>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            assertEquals(resolution, "Awaiting Response").bind()
            assertNotEmpty(comments).bind()
            assertCreationIsNotRecent(updated.toEpochMilli(), created.toEpochMilli()).bind()
            val resolveTime = changeLog
                .filter(::isAwaitingResolve)
                .last()
                .created
            val lastComment = comments.last()
            assertGreaterThan(lastComment.created, resolveTime).bind()
            assertUpdateWasNotCausedByEditingComment(
                updated.toEpochMilli(), lastComment.updated, lastComment.created
            ).bind()
            reopen().toFailedModuleEither().bind()
        }
    }

    private fun isAwaitingResolve(change: ChangeLogItem) =
        change.changedTo == "Awaiting Response"

    private fun assertCreationIsNotRecent(updated: Long, created: Long) = if ((updated - created) < 2000) {
        OperationNotNeededModuleResponse.left()
    } else {
        Unit.right()
    }

    private fun assertUpdateWasNotCausedByEditingComment(updated: Long, commentUpdated: Long, commentCreated: Long) =
        if (updated - commentUpdated < 2000 && (commentUpdated - commentCreated) > 2000) {
            OperationNotNeededModuleResponse.left()
        } else {
            Unit.right()
        }
}
