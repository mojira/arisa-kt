package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Comment

class KeepPrivateModule(private val keepPrivateTag: String?) : Module<KeepPrivateModule.Request> {
    data class Request(
        val securityLevel: String?,
        val privateLevel: String,
        val comments: List<Comment>,
        val changeLog: List<ChangeLogItem>,
        val setPrivate: () -> Either<Throwable, Unit>,
        val addSecurityComment: () -> Either<Throwable, Unit>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            assertNotNull(keepPrivateTag).bind()
            assertContainsKeepPrivateTag(comments).bind()
            assertIsPublic(securityLevel, privateLevel).bind()

            val markedTime = comments.last(::isKeepPrivateTag).created.toEpochMilli()
            val changedTime = changeLog.lastOrNull(::isSecurityChange)?.created?.toEpochMilli()
            if (changedTime != null && changedTime > markedTime) {
                addSecurityComment().toFailedModuleEither().bind()
            }
            setPrivate().toFailedModuleEither().bind()
        }
    }

    private fun isKeepPrivateTag(comment: Comment) = comment.visibilityType == "group" &&
            comment.visibilityValue == "staff" &&
            comment.body.contains(keepPrivateTag!!)

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
