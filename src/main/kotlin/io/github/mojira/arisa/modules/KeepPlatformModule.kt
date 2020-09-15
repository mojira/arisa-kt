package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Issue
import java.time.Instant
import java.time.temporal.ChronoUnit

class KeepPlatformModule(
  private val keepPlatformTag: String
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val volunteerPlatformChange = changeLog
                .filter(::isPlatformChange)
                .filter(::changedByVolunteer)
                .lastOrNull()
                ?.changedToString.getOrDefault("None")
            assertNotNull(keepPlatformTag).bind()
            assertContainsKeepPlatformTag(comments).bind()
            updatePlatforms(volunteerPlatformChange)
        }
    }

    private fun isPlatformChange(item: ChangeLogItem) =
        item.field == "Platform"

    private fun changedByVolunteer(item: ChangeLogItem) = !updateIsRecent(item) ||
            item.getAuthorGroups()?.any { it == "helper" || it == "global-moderators" || it == "staff" } ?: true

    private fun updateIsRecent(item: ChangeLogItem) =
        item
            .created
            .plus(1, ChronoUnit.DAYS)
            .isAfter(Instant.now())

    private fun assertContainsKeepPlatformTag(comments: List<Comment>) = when {
        comments.any(::isKeepPlatformTag) -> Unit.right()
        else -> OperationNotNeededModuleResponse.left()
    }

    private fun isKeepPlatformTag(comment: Comment) = comment.visibilityType == "group" &&
            comment.visibilityValue == "staff" &&
            (comment.body?.contains(keepPlatformTag) ?: false)         

    private fun String?.getOrDefault(default: String) =
        if (isNullOrBlank())
            default
        else
            this
}
