package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.User
import java.time.Instant
import java.time.temporal.ChronoUnit

const val TWO_SECONDS_IN_MILLIS = 2000

class ReopenAwaitingModule(
    private val blacklistedRoles: List<String>,
    private val blacklistedVisibilities: List<String>,
    private val softArPeriod: Long?,
    private val keepARTag: String?,
    private val message: String?
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertEquals(resolution, "Awaiting Response").bind()
            assertCreationIsNotRecent(updated.toEpochMilli(), created.toEpochMilli()).bind()

            val resolveTime = changeLog.last(::isAwaitingResolve).created
            val validComments = getValidComments(comments, resolveTime, lastRun)
            val validChangeLog = getValidChangeLog(changeLog, reporter, resolveTime)

            assertEither(
                assertNotEmpty(validComments),
                assertNotEmpty(validChangeLog)
            ).bind()

            val shouldReopen = shouldReopen(comments, validComments, reporter, resolveTime)
            if(shouldReopen) {
                reopen().toFailedModuleEither().bind()
            } else {
                assertNotNull(message)
                assertNotEquals(changeLog.sortedByDescending { it.created }.first().author.name, "arisabot")
                addComment(CommentOptions(message!!)).toFailedModuleEither().bind()
            }
        }
    }

    private fun shouldReopen(
        comments: List<Comment>,
        validComments: List<Comment>,
        reporter: User?,
        resolveTime: Instant
    ): Boolean {
        val isSoftAR = softArPeriod == null ||
                resolveTime.plus(softArPeriod, ChronoUnit.DAYS).isAfter(Instant.now())
        return comments.none(::isKeepARTag) && (isSoftAR || validComments.any { it.author.name == reporter?.name })
    }

    private fun isKeepARTag(comment: Comment) = keepARTag != null &&
            comment.visibilityType == "group" &&
            comment.visibilityValue == "staff" &&
            (comment.body?.contains(keepARTag) ?: false)

    private fun getValidComments(
        comments: List<Comment>,
        resolveTime: Instant,
        lastRun: Instant
    ): List<Comment> = comments
        .filter { it.created.isAfter(resolveTime) && it.created.isAfter(lastRun) }
        .filter {
            val roles = it.getAuthorGroups()
            roles == null || roles.intersect(blacklistedRoles).isEmpty()
        }
        .filterNot { it.visibilityType == "group" && blacklistedVisibilities.contains(it.visibilityValue) }

    private fun getValidChangeLog(
        changeLog: List<ChangeLogItem>,
        reporter: User?,
        resolveTime: Instant
    ): List<ChangeLogItem> = changeLog
        .filter { it.created.isAfter(resolveTime) }
        .filter { it.author.name == reporter?.name }
        .filter { it.field != "Comment" }

    private fun isAwaitingResolve(change: ChangeLogItem) =
        change.changedTo == "Awaiting Response"

    private fun assertCreationIsNotRecent(updated: Long, created: Long) = when {
        (updated - created) < TWO_SECONDS_IN_MILLIS -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }
}
