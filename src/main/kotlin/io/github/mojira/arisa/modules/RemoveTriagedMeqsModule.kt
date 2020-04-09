package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially2
import net.rcarz.jiraclient.Comment

data class RemoveTriagedMeqsModuleRequest(
    val priority: String?,
    val triagedTime: String?,
    val comments: List<Comment>
)

class RemoveTriagedMeqsModule(
    val updateComment: (comment: Comment, body: String) -> Either<Throwable, Unit>,
    val meqsTags: List<String>,
    val removalReason: String?
) : Module<RemoveTriagedMeqsModuleRequest> {
    override fun invoke(request: RemoveTriagedMeqsModuleRequest): Either<ModuleError, ModuleResponse> = Either.fx {
        val meqsComments = request.comments.filter(::hasMeqsTag.partially2(meqsTags))
        assertTriaged(request.priority, request.triagedTime).bind()
        assertNotEmpty(meqsComments).bind()

        tryRunAll({ updateComment(it, removeMeqsTags(it, meqsTags)) }, meqsComments).bind()
    }

    private fun hasMeqsTag(comment: Comment, meqsTags: List<String>) =
        meqsTags.any { comment.body.contains(it) }

    private fun removeMeqsTags(comment: Comment, meqsTags: List<String>): String {
        val regex = (
            "MEQS(" +
            meqsTags.joinToString("|") { it.replace("MEQS", "") } +
            ")"
        ).toRegex()
        return regex.replace(comment.body) { "MEQS_ARISA_REMOVED${it.groupValues[1]}${ if (removalReason != null) " Removal Reason: $removalReason" else "" }" }
    }

    private fun assertTriaged(priority: String?, triagedTime: String?) = when {
        priority == null && triagedTime == null -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }
}
