package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import net.rcarz.jiraclient.Comment

data class HideImpostorsModuleRequest(val comments: List<Comment>)

class HideImpostorsModule(
    private val getGroups: (String) -> Either<Throwable, List<String>>,
    val restrictCommentToGroup: (comment: Comment, body: String) -> Either<Throwable, Unit>
) : Module<HideImpostorsModuleRequest> {
    override fun invoke(request: HideImpostorsModuleRequest): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val usersWithBrackets = comments
                .filter(::userContainsBrackets)
                .filter(::userIsNotVolunteer)
                .filter(::isNotStaffRestricted)

            assertNotEmpty(usersWithBrackets).bind()
            tryRunAll({ restrictCommentToGroup(it, it.body) }, usersWithBrackets).bind()
        }
    }

    private fun userContainsBrackets(comment: Comment) = with(comment.author.displayName) {
        contains("[") && contains("]")
    }

    private fun userIsNotVolunteer(comment: Comment): Boolean {
        val groups = getGroups(comment.author.name)
        return if (groups.isLeft()) {
            false // when in doubt, assume change was done by a volunteer
        } else {
            !((groups as Either.Right).b.any { it == "helper" || it == "global-moderators" || it == "staff" })
        }
    }

    private fun isNotStaffRestricted(comment: Comment) =
        comment.visibility == null || comment.visibility.type != "group" || comment.visibility.value != "staff"
}
