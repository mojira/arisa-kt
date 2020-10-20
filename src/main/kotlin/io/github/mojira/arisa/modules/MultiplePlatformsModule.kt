package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Link
import java.time.Instant

class MultiplePlatformsModule(
    private val allowedPlatforms: List<String>,
    private val targetPlatform: String,
    private val excludedTransferredPlatforms: List<String>,
    private val keepPlatformTag: String
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertPlatformAllowed(platform, allowedPlatforms).bind()
            assertTrue(isDuplicatedWithDifferentPlatforms(platform, excludedTransferredPlatforms, issue).bind()).bind()
            assertNotKeepPlatformTag(comments).bind()
            updatePlatforms(targetPlatform)
        }
    }

    private fun isDuplicatedWithDifferentPlatforms(platform: String?, excludedPlatforms: List<String>, issue: Issue):
            Either<ModuleError, Boolean> = Either.fx {
        issue.links
            .filter(::isDuplicatedLink)
            .forEach {
                val child = it.issue.getFullIssue().toFailedModuleEither().bind()
                if (child.platform !in excludedPlatforms && child.platform != platform.getOrDefault("None")) {
                    return@fx true
                }
            }
        false
    }

    private fun isDuplicatedLink(link: Link): Boolean = link.type == "Duplicate" && !link.outwards

    private fun assertNotKeepPlatformTag(comments: List<Comment>): Either<ModuleError, ModuleResponse> {
        val volunteerComments = comments.filter(::isVolunteerComment)
        return when {
            volunteerComments.any(::isKeepPlatformTag) -> OperationNotNeededModuleResponse.left()
            else -> Unit.right()
        }
    }

    private fun isVolunteerComment(comment: Comment) = comment.visibilityType == "group" &&
            comment.visibilityValue == "staff"

    private fun isKeepPlatformTag(comment: Comment) =
            comment.body?.contains(keepPlatformTag) ?: false

    private fun assertPlatformAllowed(status: String?, allowedPlatforms: List<String>) =
        if ((status.getOrDefault("None")) in allowedPlatforms) {
            Unit.right()
        } else {
            OperationNotNeededModuleResponse.left()
        }
}
