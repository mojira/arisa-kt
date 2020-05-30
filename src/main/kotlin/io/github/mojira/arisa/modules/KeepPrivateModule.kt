package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class KeepPrivateModule(
    private val keepPrivateTag: String?,
    private val message: String
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertNotNull(keepPrivateTag).bind()
            assertContainsKeepPrivateTag(comments).bind()
            assertIsPublic(securityLevel, project.privateSecurity).bind()

            setPrivate()

            val markedTime = comments.first(::isKeepPrivateTag).created
            val changedTime = changeLog.lastOrNull(::isSecurityChange)?.created
            if (changedTime != null && changedTime.isAfter(markedTime)) {
                addComment(CommentOptions(message))
            }
        }
    }

    private fun isKeepPrivateTag(comment: Comment) = comment.visibilityType == "group" &&
            comment.visibilityValue == "staff" &&
            (comment.body?.contains(keepPrivateTag!!) ?: false)

    private fun isSecurityChange(item: ChangeLogItem) = item.field == "security"

    private fun assertContainsKeepPrivateTag(comments: List<Comment>) = when {
        comments.any(::isKeepPrivateTag) -> Unit.right()
        else -> OperationNotNeededModuleResponse.left()
    }

    private fun assertIsPublic(securityLevel: String?, privateLevel: String) = when {
        securityLevel == privateLevel -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }
}
