package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import net.rcarz.jiraclient.Comment
import net.rcarz.jiraclient.Issue

data class KeepPrivateModuleRequest(
    val issue: Issue,
    val securityLevel: String?,
    val privateLevel: String,
    val comments: List<Comment>
)

class KeepPrivateModule(
    private val changeSecurityTo: (Issue, String) -> Either<Throwable, Unit>,
    private val addSecurityComment: (Issue) -> Either<Throwable, Unit>,
    private val keepPrivateTag: String?
) : Module<KeepPrivateModuleRequest> {

    override fun invoke(request: KeepPrivateModuleRequest): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            assertNotNull(keepPrivateTag).bind()
            assertContainsKeepPrivateTag(comments).bind()
            assertIsPublic(securityLevel, privateLevel).bind()

            addSecurityComment(issue).toFailedModuleEither().bind()
            changeSecurityTo(issue, privateLevel).toFailedModuleEither().bind()
        }
    }

    private fun assertContainsKeepPrivateTag(comments: List<Comment>) = when {
        comments.any { it.body.contains(keepPrivateTag!!) } -> Unit.right()
        else -> OperationNotNeededModuleResponse.left()
    }

    private fun assertIsPublic(securityLevel: String?, privateLevel: String) = when {
        securityLevel != privateLevel -> Unit.right()
        else -> OperationNotNeededModuleResponse.left()
    }
}
