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
import java.time.temporal.ChronoUnit

const val TWO_SECONDS_IN_MILLIS = 2000

class ReopenAwaitingModule(
    private val blacklistedRoles: Set<String>,
    private val blacklistedVisibilities: Set<String>,
    private val softArPeriod: Long,
    private val keepARTag: String,
    private val onlyOPTag: String,
    private val message: String
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertEquals(resolution, "Awaiting Response").bind()
            assertCreationIsNotRecent(updated.toEpochMilli(), created.toEpochMilli()).bind()

            val resolveTime = changeLog.last(::isAwaitingResolve).created
            val validComments = getValidComments(comments, reporter, resolveTime, lastRun)
            val validChangeLog = getValidChangeLog(changeLog, reporter, resolveTime)

            assertAny(
                assertNotEmpty(validComments),
                assertNotEmpty(validChangeLog)
            ).bind()

            val shouldReopen = shouldReopen(comments, validComments, validChangeLog, reporter, resolveTime)
            if (shouldReopen) {
                reopen()
            } else {
                assertNotEquals(changeLog.maxByOrNull { it.created }?.author?.name, "arisabot")
                if (comments.none { isKeepARMessage(it) }) {
                    addRawBotComment(message)
                }
            }
        }
    }

    private fun shouldReopen(
        comments: List<Comment>,
        validComments: List<Comment>,
        validChangeLog: List<ChangeLogItem>,
        reporter: User?,
        resolveTime: Instant
    ): Boolean {
        val isSoftAR = resolveTime.plus(softArPeriod, ChronoUnit.DAYS).isAfter(Instant.now())
        val onlyOp = comments.any(::isOPTag)

        // bug report should stay in AR until a mod reopens it if keep ar flag is present
        if (comments.any(::isKeepARTag)) return false

        // reopen the bug report if:
        // the report has been updated (by the reporter) OR
        return validChangeLog.isNotEmpty() ||
            // regular users can reopen and have commented OR
            (!onlyOp && isSoftAR) ||
            // reporter has commented
            validComments.any { it.author.name == reporter?.name }
    }

    private fun isOPTag(comment: Comment) = comment.visibilityType == "group" &&
        comment.visibilityValue == "staff" &&
        (comment.body?.contains(onlyOPTag) ?: false)

    private fun isKeepARTag(comment: Comment) = comment.visibilityType == "group" &&
        comment.visibilityValue == "staff" &&
        (comment.body?.contains(keepARTag) ?: false)

    private fun isKeepARMessage(comment: Comment) =
        comment.author.name == "arisabot" && comment.body?.contains(message) ?: false

    private fun getValidComments(
        comments: List<Comment>,
        reporter: User?,
        resolveTime: Instant,
        lastRun: Instant
    ): List<Comment> = comments
        .filter { it.created.isAfter(resolveTime) && it.created.isAfter(lastRun) }
        .filter { !it.author.isNewUser() || it.author.name == reporter?.name }
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
        change.changedToString == "Awaiting Response"

    private fun assertCreationIsNotRecent(updated: Long, created: Long) = when {
        (updated - created) < TWO_SECONDS_IN_MILLIS -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }
}
