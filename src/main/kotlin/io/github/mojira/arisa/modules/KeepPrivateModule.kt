package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right

class KeepPrivateModule(private val keepPrivateTag: String?) : Module<KeepPrivateModule.Request> {
    data class Request(
        val securityLevel: String?,
        val privateLevel: String,
        val comments: List<String>,
        val setPrivate: () -> Either<Throwable, Unit>,
        val addSecurityComment: () -> Either<Throwable, Unit>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            assertNotNull(keepPrivateTag).bind()
            assertContainsKeepPrivateTag(comments).bind()
            assertIsPublic(securityLevel, privateLevel).bind()

            addSecurityComment().toFailedModuleEither().bind()
            setPrivate().toFailedModuleEither().bind()
        }
    }

    private fun assertContainsKeepPrivateTag(comments: List<String>) = when {
        comments.any { it.contains(keepPrivateTag!!) } -> Unit.right()
        else -> OperationNotNeededModuleResponse.left()
    }

    private fun assertIsPublic(securityLevel: String?, privateLevel: String) = when {
        securityLevel == privateLevel -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }
}
