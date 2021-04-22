package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially1
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Link
import io.github.mojira.arisa.domain.LinkedIssue
import java.time.Instant
import java.time.temporal.ChronoUnit

@Suppress("TooManyFunctions", "LongParameterList")
class DuplicateMessageModule(
    private val commentDelayMinutes: Long,
    private val message: String,
    private val forwardMessage: String,
    private val ticketMessages: Map<String, String>,
    private val privateMessage: String,
    private val preventMessageTags: List<String>,
    private val resolutionMessages: Map<String, String>
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertLinkedAfterLastRun(changeLog, lastRun).bind()

            assertNotNull(preventMessageTags).bind()
            assertNotContainsPreventMessageTag(comments).bind()

            val historicalParentKeys = changeLog
                .filter(::isAddingDuplicateLink)
                .map { it.changedTo!! }
            val visibleComments = comments
                .filter(::isPublicComment)
            assertNoneIsMentioned(visibleComments, historicalParentKeys).bind()

            val parents = links
                .filter(::isDuplicatesLink)
                .map { it.issue }
                .sortedBy { it.key }
            assertNotEmpty(parents).bind()

            val parentKey = parents.getCommonFieldOrNull { it.key }
            var messageKey = ticketMessages[parentKey]
            if (messageKey == null) {
                val fullParentEitherList = parents
                    .map { it.getFullIssue() }
                fullParentEitherList.toFailedModuleEither().bind()
                val fullParents = fullParentEitherList
                    .map { (it as Either.Right).b }

                messageKey = getPrivateMessageOrNull(fullParents)
                    ?: getResolutionMessageOrNull(fullParents) ?: getForwardResolvedOrNot(issue, fullParents)
            }

            val filledText = parents.getFilledText()

            addComment(CommentOptions(messageKey, filledText))
        }
    }

    private fun assertLinkedAfterLastRun(
        changeLog: List<ChangeLogItem>,
        lastRun: Instant
    ): Either<ModuleError, ModuleResponse> = Either.fx {
        val lastChange = changeLog
            .lastOrNull(::isAddingDuplicateLink)
        assertNotNull(lastChange).bind()
        assertAfter(lastChange!!.created, lastRun.minus(commentDelayMinutes, ChronoUnit.MINUTES)).bind()
    }

    private fun isAddingDuplicateLink(item: ChangeLogItem) = item.field == "Link" && item.changedTo != null &&
            item.changedToString != null && item.changedToString.startsWith("This issue duplicates ")

    private fun List<LinkedIssue>.getFilledText() = this
        .map { it.key }
        .run {
            when (size) {
                1 -> get(0)
                2 -> "${get(0)}* and *${get(1)}"
                else -> "${subList(0, lastIndex).joinToString("*, *")}*, and *${last()}"
            }
        }

    private fun <V, F> List<V>.getCommonFieldOrNull(getField: (V) -> F?) =
        if (this.any { getField(it) != getField(this[0]) }) {
            null
        } else {
            getField(this[0])
        }

    private fun getPrivateMessageOrNull(issues: List<Issue>): String? {
        val parentSecurity = issues.getCommonFieldOrNull { it.securityLevel }
        return privateMessage.takeIf { parentSecurity != null }
    }

    private fun getResolutionMessageOrNull(issues: List<Issue>): String? {
        val parentResolution = issues.getCommonFieldOrNull { it.resolution }
        return resolutionMessages[parentResolution]
    }

    private fun getForwardResolvedOrNot(child: Issue, issues: List<Issue>): String {
        val childCreationTime = child.created
        val parentCreationTime = issues.getCommonFieldOrNull { it.created }
        return if (
            childCreationTime != null &&
            parentCreationTime != null &&
            childCreationTime.isBefore(parentCreationTime)
        ) {
            forwardMessage
        } else {
            message
        }
    }

    private fun assertNoneIsMentioned(comments: List<Comment>, parents: List<String>) =
        assertTrue(parents.any(::hasBeenMentioned.partially1(comments))).invert()

    private fun hasBeenMentioned(comments: List<Comment>, key: String) =
        comments.any { it.body?.contains(key) ?: false }

    private fun isPublicComment(comment: Comment) =
        comment.visibilityType == null

    private fun isDuplicatesLink(link: Link) =
        link.type.toLowerCase() == "duplicate" && link.outwards

    private fun isPreventMessageTag(comment: Comment) = comment.visibilityType == "group" &&
            comment.visibilityValue == "staff" &&
            preventMessageTags.any { comment.body!!.contains(it) }

    private fun assertNotContainsPreventMessageTag(comments: List<Comment>) = when {
        comments.any(::isPreventMessageTag) -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }
}
