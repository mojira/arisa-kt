package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Version
import java.time.Instant

class FutureVersionModule(
    private val messageFull: String,
    private val messagePanel: String
) : Module() {
    override fun execute(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val addedVersions = getVersionsLatelyAddedByNonStaff(lastRun)
            val removeFutureVersions = affectedVersions
                .filter(::isFutureVersion)
                .filter { it.id in addedVersions }
                .map { removedAffectedVersions.add(it) }
            assertNotEmpty(removeFutureVersions).bind()

            val latestVersion = project.versions.lastOrNull(::isReleasedVersion)
            assertNotNull(latestVersion).bind()

            if (affectedVersions.size > removeFutureVersions.size) {
                addComment(messagePanel)
            } else {
                addedAffectedVersions.add(latestVersion!!)
                if (resolution == null || resolution == "Unresolved") {
                    resolution = "Awaiting Response"
                    addComment(messageFull)
                } else {
                    addComment(messagePanel)
                }
            }
        }
    }

    private fun Issue.getVersionsLatelyAddedByNonStaff(lastRun: Instant): List<String> =
        if (created.isAfter(lastRun)) {
            if (isStaff(reporter?.groups.orEmpty())) {
                emptyList()
            } else {
                affectedVersions.map { ver -> ver.id }
            }
        } else {
            changeLog
                .asSequence()
                .filter { it.created.isAfter(lastRun) }
                .filter { it.field.toLowerCase() == "version" }
                .filterNot { isStaff(it.author.groups) }
                .mapNotNull { it.changedTo }
                .toList()
        }

    private fun isStaff(groups: List<String>?) = "staff" in (groups ?: emptyList())

    private fun isFutureVersion(version: Version) =
        !version.released && !version.archived

    private fun isReleasedVersion(version: Version) =
        version.released && !version.archived
}
