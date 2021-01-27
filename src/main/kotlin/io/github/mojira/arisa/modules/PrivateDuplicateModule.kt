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
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertNotNull(keepPrivateTag).bind()
            assertIsPublic(securityLevel, project.privateSecurity).bind()
            issue.links
                .filter(::isDuplicatesLink)
                .forEach {
                    assertIsPublic(securityLevel, project.privateSecurity).bind()
                    val parent = it.issue.getFullIssue().toFailedModuleEither().bind()
                    assertParentPrivate(parent.securityLevel, parent.project.privateSecurity).bind()
                    setPrivate()
                    if (parentHasKeepPrivateTag(parent)) {
                        addRawRestrictedComment(keepPrivateTag!!, "staff")
                    }
                }
        }
    }

    private fun isKeepPrivateTag(comment: Comment) = comment.visibilityType == "group" &&
            comment.visibilityValue == "staff" &&
            (comment.body?.contains(keepPrivateTag!!) ?: false)

    private fun parentHasKeepPrivateTag(parent: Issue): Boolean = parent.comments.any(::isKeepPrivateTag)

    private fun isDuplicatesLink(link: Link): Boolean = link.type == "Duplicates" && link.outwards

    private fun assertIsPublic(securityLevel: String?, privateLevel: String) = when {
        securityLevel == privateLevel -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }
    private fun assertParentPrivate(securityLevel: String?, privateLevel: String) = when {
        securityLevel == privateLevel -> OperationNotNeededModuleResponse.left()
        else -> Unit.right()
    }
}
