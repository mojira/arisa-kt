package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Version
import java.time.Instant

/**
 * Module for adding a message comment when an issue has a specific version. However, unlike other modules
 * this module does not perform any other action, it neither removes the version nor resolves the issue.
 * This module is intended for versions which are often erroneously added by users.
 */
class AffectedVersionMessageModule(
    private val versionIdMessageMap: Map<String, String>
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertTrue(created.isAfter(lastRun)).bind()
            // Ignore when created by staff
            assertFalse(wasCreatedByStaff()).bind()

            // Note: Don't check changelog; assume that versions added after issue was created were added correctly
            // (though this check here still covers them in case Arisa has not run in between)
            val message = affectedVersions.asSequence()
                .map(Version::id)
                .mapNotNull(versionIdMessageMap::get)
                .firstOrNull()

            assertNotNull(message).bind()
            addComment(CommentOptions(message!!))
        }
    }

    private fun Issue.wasCreatedByStaff() =
        reporter.getGroups.invoke()?.any { it == "helper" || it == "global-moderators" || it == "staff" } ?: false
}
