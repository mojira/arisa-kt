package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.cloud.CloudIssue
import io.github.mojira.arisa.domain.cloud.CloudLink
import java.time.Instant

class PrivateDuplicateModule(
    private val keepPrivateTag: String?
) : CloudModule {
    override fun invoke(issue: CloudIssue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertNotNull(keepPrivateTag).bind()
            assertNull(securityLevel).bind()

            val duplicatedReports = links
                .filter(::isDuplicatesLink)
                .map { it.issue.getFullIssue().toFailedModuleEither().bind() }
            assertGreaterThan(duplicatedReports.size, 0).bind()
            duplicatedReports.forEach {
                assertNotNull(it.securityLevel).bind()
                setPrivate()
                if (parentHasKeepPrivateTag(it)) {
                    addRawRestrictedComment(keepPrivateTag!!, "staff")
                }
            }
        }
    }

    private fun isKeepPrivateTag(comment: Comment) = comment.visibilityType == "group" &&
        comment.visibilityValue == "staff" &&
        (comment.body?.contains(keepPrivateTag!!) ?: false)

    private fun parentHasKeepPrivateTag(parent: CloudIssue): Boolean = parent.comments.any(::isKeepPrivateTag)

    private fun isDuplicatesLink(link: CloudLink): Boolean = link.type == "Duplicate" && link.outwards
}
