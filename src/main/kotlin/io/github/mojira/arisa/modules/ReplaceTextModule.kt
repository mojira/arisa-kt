package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class ReplaceTextModule(
    private val replacements: List<Pair<Regex, String>> = listOf(
        """\[([A-Z]+-\d+)\|https?://bugs\.mojang\.com/browse/\1/?(?![\d?/#])]""".toRegex() to "$1",
        """\[([A-Z]+-\d+)\|https?://bugs\.mojang\.com/projects/[A-Z]+/issues/\1/?(?![\d?/#])]""".toRegex() to "$1",
        """(?<!\|)https?://bugs\.mojang\.com/browse/([A-Z]+-\d+)/?(?![\d?/#])""".toRegex() to "$1",
        """(?<!\|)https?://bugs\.mojang\.com/projects/[A-Z]+/issues/([A-Z]+-\d+)/?(?![\d?/#])""".toRegex() to "$1",
        "(http://i.imgur.com)".toRegex() to "https://i.imgur.com"
    )
) : Module() {
    data class Request(
        val lastRun: Instant,
        val description: String?,
        val comments: List<Comment>,
        val updateDescription: (description: String) -> Either<Throwable, Unit>
    )

    override fun execute(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val needUpdateDescription = created.isAfter(lastRun) &&
                    description != null &&
                    needReplacement(description)

            val filteredComments = comments
                .filter { createdAfterLastRun(it.created, lastRun) }
                .filter { needReplacement(it.body) }

            assertEither(
                assertTrue(needUpdateDescription),
                assertNotEmpty(filteredComments)
            ).bind()

            if (needUpdateDescription) {
                updateDescription(replace(description!!))
            }

            filteredComments.forEach {
                it.update(replace(it.body!!))
            }
        }
    }

    private fun createdAfterLastRun(updated: Instant, lastRun: Instant) = updated.isAfter(lastRun)

    private fun needReplacement(text: String?) = replacements.any { (regex, _) -> text?.contains(regex) ?: false }

    private fun replace(text: String): String = replacements.fold(
        text,
        { str, (regex, replacement) -> str.replace(regex, replacement) }
    )
}
