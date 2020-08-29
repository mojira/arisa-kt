package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Issue
import java.time.Instant

class RemoveVersionModule : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val addedVersions = getExtraVersionsLatelyAddedByNonVolunteers(lastRun)
            val removeAddedVersions = affectedVersions
                .filter { it.id in addedVersions }
                .map { it.remove }
            assertNotEmpty(removeAddedVersions).bind()
            removeAddedVersions.forEach(::run)
        }
    }

    private fun Issue.getExtraVersionsLatelyAddedByNonVolunteers(lastRun: Instant): List<String> =
        if (created.isAfter(lastRun)) {
            if (isVolunteer(reporter?.getGroups?.invoke()) || resolution == "Unresolved") {
                emptyList()
            } else {
                affectedVersions.map { ver -> ver.id }
            }
        } else {
            changeLog
                .asSequence()
                .filter { it.created.isAfter(lastRun) }
                .filter { it.field.toLowerCase() == "version" }
                .filterNot { isVolunteer(it.getAuthorGroups()) }
                .mapNotNull { it.changedTo }
                .toList()
        }

    private fun isVolunteer(groups: List<String>?) = groups?.any { it == "helper" || it == "global-moderators" || it == "staff" } ?: false
}
