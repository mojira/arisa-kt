package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.User
import java.time.Instant

const val TWO_SECONDS_IN_MILLIS = 2000

class ReopenAwaitingModule(
    private val blacklistedRoles: List<String>,
    private val blacklistedVisibilities: List<String>,
    private val keepARTag: String?
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertEquals(resolution, "Awaiting Response").bind()
            assertCreationIsNotRecent(updated.toEpochMilli(), created.toEpochMilli()).bind()
            assertShouldNotKeepAR(comments).bind()

            val resolveTime = changeLog.last(::isAwaitingResolve).created

            assertEither(
                assertUpdatedByAddingComment(comments, resolveTime, lastRun),
                assertUpdatedByReporterChangingTicket(changeLog, reporter, resolveTime)
            ).bind()

            reopen()
        }
    }

    private fun assertShouldNotKeepAR(comments: List<Comment>) = assertNotContains(comments, ::isKeepARTag)

    private fun isKeepARTag(comment: Comment) = keepARTag != null &&
            comment.visibilityType == "group" &&
            comment.visibilityValue == "staff" &&
            (comment.body?.contains(keepARTag) ?: false)

    private fun assertUpdatedByAddingComment(
        comments: List<Comment>,
        resolveTime: Instant,
        lastRun: Instant
    ): Either<OperationNotNeededModuleResponse, ModuleResponse> = Either.fx {
        val validComments = comments
            .filter { it.created.isAfter(resolveTime) && it.created.isAfter(lastRun) }
            .filter {
                val roles = it.getAuthorGroups()
                roles == null || roles.intersect(blacklistedRoles).isEmpty()
            }
            .filterNot { it.visibilityType == "group" && blacklistedVisibilities.contains(it.visibilityValue) }
        assertNotEmpty(validComments).bind()
    }

    private fun assertUpdatedByReporterChangingTicket(
        changeLog: List<ChangeLogItem>,
        reporter: User?,
        resolveTime: Instant
    ): Either<OperationNotNeededModuleResponse, ModuleResponse> = Either.fx {
        val validChanges = changeLog
            .filter { it.created.isAfter(resolveTime) }
            .filter { it.author.name == reporter?.name }
            .filter { it.field != "Comment" }
        assertNotEmpty(validChanges).bind()
    }

    private fun isAwaitingResolve(change: ChangeLogItem) =
        change.changedTo == "Awaiting Response"

    private fun assertCreationIsNotRecent(updated: Long, created: Long) = when {
        (updated - created) < TWO_SECONDS_IN_MILLIS -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }
}
