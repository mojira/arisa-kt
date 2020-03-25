package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import net.rcarz.jiraclient.Comment

data class RemoveNonStaffMeqsModuleRequest(val comments: List<Comment>)

class RemoveNonStaffMeqsModule(
    val updateComment: (comment: Comment, body: String) -> Either<Throwable, Unit>,
    val isStaffRestricted: (comment: Comment) -> Either<Throwable, Boolean>,
    val restrictToHelper: (comment: Comment) -> Either<Throwable, Unit>
) : Module<RemoveNonStaffMeqsModuleRequest> {
    override fun invoke(request: RemoveNonStaffMeqsModuleRequest): Either<ModuleError, ModuleResponse> = Either.fx {
        val meqsComments = request.comments
            .filter(::hasMeqsTag)
            .filter(::isNotStaffRestricted)
        assertNotEmpty(meqsComments).bind()

        tryRunAll({ updateComment(it, removeMeqsTags(it)) }, meqsComments).bind()
        tryRunAll(restrictToHelper, meqsComments).bind()
    }

    private fun hasMeqsTag(comment: Comment) =
        comment.body.contains("""MEQS_[A-Z_]+""".toRegex())

    private fun isNotStaffRestricted(comment: Comment): Boolean {
        val result = isStaffRestricted(comment)
        return result.isRight() && !(result as Either.Right).b
    }

    private fun removeMeqsTags(comment: Comment): String {
        val regex = """MEQS(_[A-Z_]+)""".toRegex()
        return regex.replace(comment.body) { it.groupValues[1] }
    }
}
