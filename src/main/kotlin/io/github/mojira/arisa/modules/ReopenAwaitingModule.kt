package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Comment
import java.time.Instant

class ReopenAwaitingModule(
    private val blacklistedRoles: List<String>,
    private val blacklistedVisibilities: List<String>
) : Module<ReopenAwaitingModule.Request> {
    data class Request(
        val resolution: String?,
        val lastRun: Instant,
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
            val resolveTime = changeLog.last(::isAwaitingResolve)
                .created
            val lastComment = comments.last()
            assertAfter(lastComment.created, resolveTime).bind()
            assertAfter(lastComment.created, lastRun).bind()
            assertCommentIsNotRestrictedToABlacklistedLevel(lastComment.visibilityType, lastComment.visibilityValue).bind()
            assertCommentWasNotAddedByABlacklistedRole(lastComment.getAuthorGroups()).bind()

            reopen().toFailedModuleEither().bind()
        }
    }

    private fun isAwaitingResolve(change: ChangeLogItem) =
        change.changedTo == "Awaiting Response"

    private fun assertCreationIsNotRecent(updated: Long, created: Long) = when {
        (updated - created) < 2000 -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }

    private fun assertCommentWasNotAddedByABlacklistedRole(roles: List<String>?) = when {
        roles == null -> Unit.right()
        roles.none { it in blacklistedRoles } -> Unit.right()
        else -> OperationNotNeededModuleResponse.left()
    }

    private fun assertCommentIsNotRestrictedToABlacklistedLevel(visibilityType: String?, visibilityValue: String?) = when {
        visibilityType != "group" -> Unit.right()
        blacklistedVisibilities.none { it == visibilityValue } -> Unit.right()
        else -> OperationNotNeededModuleResponse.left()
    }
}
