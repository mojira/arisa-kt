package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.partially2

class ReplaceCommentModule(
    private val replacements: Map<Regex, String> = mapOf(
        Regex("\\[([A-Z]+-\\d+)\\|https?://bugs\\.mojang\\.com/browse/\\1(?:\\?[\\w%=&]*)?\\]") to "$1",
        Regex("\\[([A-Z]+-\\d+)\\|https?://bugs\\.mojang\\.com/projects/[A-Z]+/issues/\\1(?:\\?[\\w%=&]*)?\\]") to "$1",
        Regex("(?<=[^\\|])https?://bugs\\.mojang\\.com/browse/([A-Z]+-\\d+)(?:\\?[\\w%=&]*)?") to "$1",
        Regex("(?<=[^\\|])https?://bugs\\.mojang\\.com/projects/[A-Z]+/issues/([A-Z]+-\\d+)(?:\\?[\\w%=&]*)?") to "$1"
    )
) : Module<ReplaceCommentModule.Request> {
    data class Comment(
        val updated: Long,
        val body: String,
        val updateCommentBody: (body: String) -> Either<Throwable, Unit>
    )

    data class Request(
        val lastRun: Long,
        val comments: List<Comment>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val filteredComments = comments
                .filter(::updatedAfterLastRun.partially2(lastRun))
                .filter(::needReplacement)

            assertNotEmpty(filteredComments).bind()

            filteredComments.forEach {
                it.updateCommentBody(replace(it)).toFailedModuleEither().bind()
            }
        }
    }

    private fun updatedAfterLastRun(comment: Comment, lastRun: Long) = comment.updated > lastRun

    private fun needReplacement(comment: Comment) = replacements.entries.any { (regex) -> comment.body.contains(regex) }

    private fun replace(comment: Comment): String = replacements.entries.fold(
        comment.body,
        { str, (regex, replacement) -> str.replace(regex, replacement) }
    )
}
