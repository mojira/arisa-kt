package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.partially1
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Link
import io.github.mojira.arisa.domain.LinkedIssue
import java.time.Instant

class DuplicateMessageModule(
    private val message: String,
    private val ticketMessages: Map<String, String>,
    private val privateMessage: String?,
    private val resolutionMessages: Map<String, String>
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val parents = links
                .filter(::isDuplicatesLink)
                .map { it.issue }
                .sortedBy { it.key }
            assertNotEmpty(parents).bind()

            val visibleComments = comments
                .filter(::isPublicComment)
            assertNotAllMentionedBefore(visibleComments, parents).bind()

            val parentKey = parents.getCommonFieldOrNull { it.key }
            var messageKey = ticketMessages[parentKey]
            if (messageKey == null) {
                val fullParentEitherList = parents
                    .map { it.getFullIssue() }
                fullParentEitherList.toFailedModuleEither().bind()
                val fullParents = fullParentEitherList
                    .map { (it as Either.Right).b }

                messageKey = getPrivateMessageOrNull(fullParents) ?: getResolutionMessageOrNull(fullParents) ?: message
            }

            val filledText = parents.getFilledText()

            addComment(CommentOptions(messageKey, filledText)).toFailedModuleEither().bind()
        }
    }

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

    private fun assertNotAllMentionedBefore(comments: List<Comment>, parents: List<LinkedIssue>) =
        assertNotEmpty(parents.filter(::hasNotBeenMentioned.partially1(comments)))

    private fun hasNotBeenMentioned(comments: List<Comment>, issue: LinkedIssue) =
        comments.none { it.body.contains(issue.key) }

    private fun isPublicComment(comment: Comment) =
        comment.visibilityType == null

    private fun isDuplicatesLink(link: Link) =
        link.type.toLowerCase() == "duplicate" && link.outwards
}
