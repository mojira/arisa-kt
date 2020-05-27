package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.complement
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Version
import io.github.mojira.arisa.domain.CommentOptions
import java.time.Instant

class FutureVersionModule(
    private val message: String
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val removeFutureVersions = affectedVersions
                .filter(::isFutureVersion)
                .map { it.remove }
            assertNotEmpty(removeFutureVersions).bind()

            val latestVersion = project.versions.lastOrNull(::isFutureVersion.complement())
            assertNotNull(latestVersion).bind()

            latestVersion!!.add().toFailedModuleEither().bind()
            tryRunAll(removeFutureVersions).bind()
            addComment(CommentOptions(message)).toFailedModuleEither().bind()
            resolveAsAwaitingResponse().toFailedModuleEither().bind()
        }
    }

    private fun isFutureVersion(version: Version) =
        !version.released && !version.archived
}
