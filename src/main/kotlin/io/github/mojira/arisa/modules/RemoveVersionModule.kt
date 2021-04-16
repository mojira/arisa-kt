package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.User
import java.time.Instant

const val MAX_NUMBER_OF_VERSION_CHANGES = 5

class RemoveVersionModule(
    private val message: String
) : Module() {
    override fun execute(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val addedVersions = getExtraVersionsLatelyAddedByNonVolunteers(lastRun)
            val removeAddedVersions = affectedVersions
                .filter { it.id in addedVersions }
            assertNotEmpty(removeAddedVersions).bind()
            removedAffectedVersions.addAll(removeAddedVersions)
            if (calledMoreThanMaxTimes(changeLog)) {
                reporter = User("NoUser", "NoUser", emptyList())
            }
            addComment(message)
        }
    }

    private fun calledMoreThanMaxTimes(changeLog: List<ChangeLogItem>): Boolean {
        return changeLog
            .asSequence()
            .filter { it.author.name == "arisabot" }
            .filter { it.field == "Version" }
            .filter { it.changedFrom != null }
            .filter { it.changedTo == null } // To only get deleted versions
            .count() >= MAX_NUMBER_OF_VERSION_CHANGES
    }

    private fun Issue.getExtraVersionsLatelyAddedByNonVolunteers(lastRun: Instant): List<String> =
        if (created.isAfter(lastRun)) {
            if (isVolunteer(reporter?.groups.orEmpty())) {
                emptyList()
            } else {
                affectedVersions.map { ver -> ver.id }
            }
        } else {
            changeLog
                .asSequence()
                .filter { it.created.isAfter(lastRun) }
                .filter { it.field.toLowerCase() == "version" }
                .filterNot { isVolunteer(it.author.groups) }
                .mapNotNull { it.changedTo }
                .toList()
        }

    private fun isVolunteer(groups: List<String>?) =
        groups?.any { it == "helper" || it == "global-moderators" || it == "staff" } ?: false
}
