package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Version
import java.time.Instant

class FutureVersionModule(
    private val messageFull: String,
    private val messagePanel: String,
    private val resolveAsInvalidMessages: Map<String, String>
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val addedVersions = getVersionsLatelyAddedByNonStaff(lastRun)
            val futureVersionsToRemove = affectedVersions
                .filter(::isFutureVersion)
                .filter { it.id in addedVersions }
            assertNotEmpty(futureVersionsToRemove).bind()

            val latestVersion = project.versions.lastOrNull(::isReleasedVersion)
            assertNotNull(latestVersion).bind()

            if (affectedVersions.size > futureVersionsToRemove.size) {
                addComment(CommentOptions(messagePanel))
            } else {
                // Cannot leave affected versions empty so need to choose latest version, but prompt user
                // to choose correct version
                issue.addAffectedVersion(latestVersion!!)
                if (resolution == null || resolution == "Unresolved") {
                    val invalidMessage = futureVersionsToRemove.asSequence()
                        .mapNotNull { resolveAsInvalidMessages[it.id] }
                        .firstOrNull()

                    if (invalidMessage == null) {
                        resolveAsAwaitingResponse()
                        addComment(CommentOptions(messageFull))
                    } else {
                        resolveAsInvalid()
                        addComment(CommentOptions(invalidMessage))
                    }
                } else {
                    addComment(CommentOptions(messagePanel))
                }
            }

            futureVersionsToRemove.forEach(removeAffectedVersion)
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

    private fun isReleasedVersion(version: Version) =
        version.released && !version.archived
}
