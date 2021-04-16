package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import java.time.Instant
import java.time.temporal.ChronoUnit

class KeepPlatformModule(
    private val keepPlatformTag: String
) : Module() {
    override fun execute(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val volunteerPlatformChange = changeLog
                .filter(::isPlatformChange).lastOrNull(::changedByVolunteer)
                ?.changedToString.getOrDefault("None")
            assertNotEquals(platform.getOrDefault("None"), volunteerPlatformChange).bind()
            assertNotNull(keepPlatformTag).bind()
            assertContainsKeepPlatformTag(comments).bind()
            platform = volunteerPlatformChange
        }
    }

    private fun isPlatformChange(item: ChangeLogItem) =
        item.field == "Platform"

    private fun changedByVolunteer(item: ChangeLogItem) = !updateIsRecent(item) ||
            item.author.groups.any { it == "helper" || it == "global-moderators" || it == "staff" } ?: true

    private fun updateIsRecent(item: ChangeLogItem) =
        item
            .created
            .plus(1, ChronoUnit.DAYS)
            .isAfter(Instant.now())

    private fun assertContainsKeepPlatformTag(comments: List<Comment>): Either<ModuleError, ModuleResponse> {
        val volunteerComments = comments.filter(::isVolunteerComment)
        return when {
            volunteerComments.any(::isKeepPlatformTag) -> Unit.right()
            else -> OperationNotNeededModuleResponse.left()
        }
    }

    private fun isVolunteerComment(comment: Comment) = comment.visibilityType == "group" &&
        comment.visibilityValue == "staff"

    private fun isKeepPlatformTag(comment: Comment) =
            comment.body?.contains(keepPlatformTag) ?: false
}
