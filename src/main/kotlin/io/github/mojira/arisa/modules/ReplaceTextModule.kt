package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx

class ReplaceTextModule(
    private val replacements: List<Pair> = listOf(
        Pair("""\[([A-Z]+-\d+)\|https?://bugs\.mojang\.com/browse/\1(?:\?[\w%=&]*)?\]""".toRegex()),
        Pair("""\[([A-Z]+-\d+)\|https?://bugs\.mojang\.com/projects/[A-Z]+/issues/\1(?:\?[\w%=&]*)?\]""".toRegex()),
        Pair("""(?<=[^\|])https?://bugs\.mojang\.com/browse/([A-Z]+-\d+)(?:\?[\w%=&]*)?""".toRegex()),
        Pair("""(?<=[^\|])https?://bugs\.mojang\.com/projects/[A-Z]+/issues/([A-Z]+-\d+)(?:\?[\w%=&]*)?""".toRegex())
    )
) : Module<ReplaceTextModule.Request> {
    data class Pair(
        val regex: Regex,
        val replacement: String = "$1"
    )

    data class Comment(
        val updated: Long,
        val body: String,
        val updateBody: (body: String) -> Either<Throwable, Unit>
    )

    data class Request(
        val lastRun: Long,
        val description: String?,
        val comments: List<Comment>,
        val updateDescription: (description: String) -> Either<Throwable, Unit>
    )

    override fun invoke(request: Request): Either<ModuleError, ModuleResponse> = with(request) {
        Either.fx {
            val needUpdateDescription = description != null && needReplacement(description)

            val filteredComments = comments
                .filter { updatedAfterLastRun(it.updated, lastRun) }
                .filter { needReplacement(it.body) }

            assertOr(
                assertTrue(needUpdateDescription),
                assertNotEmpty(filteredComments)
            ).bind()

            if (needUpdateDescription) {
                updateDescription(replace(description!!)).toFailedModuleEither().bind()
            }

            filteredComments.forEach {
                it.updateBody(replace(it.body)).toFailedModuleEither().bind()
            }
        }
    }

    private fun updatedAfterLastRun(updated: Long, lastRun: Long) = updated > lastRun

    private fun needReplacement(text: String) = replacements.any { (regex, _) -> text.contains(regex) }

    private fun replace(text: String): String = replacements.fold(
        text,
        { str, (regex, replacement) -> str.replace(regex, replacement) }
    )
}
