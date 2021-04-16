package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Link
import java.time.Instant

class PrivateDuplicateModule(
    private val keepPrivateTag: String?
) : Module() {
    override fun execute(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertNotNull(keepPrivateTag).bind()
            assertIsPublic(securityLevel, project.privateSecurity).bind()
            val duplicatedReports = links
                .filter(::isDuplicatesLink)
                .mapNotNull { it.issue?.issue?.get() }
            assertGreaterThan(duplicatedReports.size, 0).bind()
            duplicatedReports.forEach {
                assertParentPrivate(it.securityLevel, it.project.privateSecurity).bind()
                securityLevel = it.project.privateSecurity
                if (parentHasKeepPrivateTag(it)) {
                    addRawComment(keepPrivateTag!!, "staff")
                }
            }
        }
    }

    private fun isKeepPrivateTag(comment: Comment) = comment.visibilityType == "group" &&
            comment.visibilityValue == "staff" &&
            (comment.body.contains(keepPrivateTag!!) ?: false)

    private fun parentHasKeepPrivateTag(parent: Issue): Boolean = parent.comments.any(::isKeepPrivateTag)

    private fun isDuplicatesLink(link: Link): Boolean = link.type == "Duplicate" && link.outwards

    private fun assertIsPublic(securityLevel: String?, privateLevel: String) =
        if (securityLevel == privateLevel)
            OperationNotNeededModuleResponse.left()
        else
            Unit.right()

    private fun assertParentPrivate(securityLevel: String?, privateLevel: String) =
        if (securityLevel == privateLevel)
            Unit.right()
        else
            OperationNotNeededModuleResponse.left()
}
