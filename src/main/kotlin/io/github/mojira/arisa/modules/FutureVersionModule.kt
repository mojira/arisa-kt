package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.complement
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Version
import java.time.Instant

class FutureVersionModule(
    private val messageFull: String,
    private val messagePanel: String
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val addedVersions = getVersionsLatelyAddedByNonStaff(lastRun)
            val removeFutureVersions = affectedVersions
                .filter(::isFutureVersion)
                .filter { it.id in addedVersions }
                .map { it.remove }
            assertNotEmpty(removeFutureVersions).bind()

            val latestVersion = project.versions.lastOrNull(::isFutureVersion.complement())
            assertNotNull(latestVersion).bind()

            if (affectedVersions.size > removeFutureVersions.size) {
                addComment(CommentOptions(messagePanel))
            } else {
                latestVersion!!.add()
                if (resolution == null || resolution == "Unresolved") {
                                        resolveAsAwaitingResponse()
                }
                addComment(CommentOptions(messagePanel))
                    resolveAsAwaitingResponse()
                    addComment(CommentOptions(messageFull))
                } else {
                    addComment(CommentOptions(messagePanel))
                }
            }
            removeFutureVersions.forEach(::run)
        }
    }

    private fun Issue.getVersionsLatelyAddedByNonStaff(lastRun: Instant): List<String> =
        if (created.isAfter(lastRun)) {
            if (isStaff(reporter?.getGroups?.invoke())) {
                emptyList()
            } else {
                affectedVersions.map { ver -> ver.id }
            }
        } else {
            changeLog
                .asSequence()
                .filter { it.created.isAfter(lastRun) }
                .filter { it.field.toLowerCase() == "version" }
                .filterNot { isStaff(it.getAuthorGroups()) }
                .mapNotNull { it.changedTo }
                .toList()
        }

    private fun isStaff(groups: List<String>?) = "staff" in (groups ?: emptyList())

    private fun isFutureVersion(version: Version) =
        !version.released && !version.archived
}
