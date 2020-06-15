package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.complement
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Version
import io.github.mojira.arisa.domain.CommentOptions
import java.time.Instant

class FutureVersionModule(
    private val message: String
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val addedVersions = getLatelyAddedVersions(lastRun)
            val removeFutureVersions = affectedVersions
                .filter(::isFutureVersion)
                .filter { it.id in addedVersions }
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

    private fun Issue.getLatelyAddedVersions(lastRun: Instant): List<String> {
        return changeLog
            .asSequence()
            .filter { it.created.isAfter(lastRun) }
            .filter { it.field.toLowerCase() == "version" }
            .filterNot { "staff" in (it.getAuthorGroups() ?: emptyList()) }
            .mapNotNull { it.changedTo }
            .toList()
    }

    private fun isFutureVersion(version: Version) =
        !version.released && !version.archived
}
