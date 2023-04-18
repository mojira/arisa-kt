package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Link
import java.time.Instant

@Suppress("TooManyFunctions")
class MultiplePlatformsModule(
    private val dungeonsPlatformWhitelist: List<String>,
    private val legendsPlatformWhitelist: List<String>,
    private val platformWhitelist: List<String>,
    private val targetPlatform: String,
    private val transferredPlatformBlacklist: List<String>,
    private val keepPlatformTag: String
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val platformValue = getPlatformValue()
            assertPlatformWhitelisted(platformValue, getPlatformWhitelist(project.key)).bind()
            assertTrue(
                isDuplicatedWithDifferentPlatforms(
                    platformValue,
                    transferredPlatformBlacklist,
                    issue,
                    lastRun
                ).bind()
            ).bind()
            assertNotKeepPlatformTag(comments).bind()
            if (project.key == "MCD") {
                updateDungeonsPlatform(targetPlatform)
            } else if (project.key == "MCLG"){
                updateLegendsPlatform(targetPlatform)
            } else {
                updatePlatform(targetPlatform)
            }
        }
    }

    private fun getPlatformWhitelist(project: String) =
        if (project == "MCD") dungeonsPlatformWhitelist
            else if (project == "MCLG") legendsPlatformWhitelist else platformWhitelist

    private fun Issue.getPlatformValue() = if (project.key == "MCD") dungeonsPlatform
        else if (project.key == "MCLG") legendsPlatform else platform

    private fun isDuplicatedWithDifferentPlatforms(
        platform: String?,
        blacklist: List<String>,
        issue: Issue,
        lastRun: Instant
    ): Either<ModuleError, Boolean> = Either.fx {
        val expectedDuplicateText = "This issue duplicates ${issue.key}"
        issue.links
            .filter(::isDuplicatedLink)
            .forEach {
                val child = it.issue.getFullIssue().toFailedModuleEither().bind()
                if (child.resolution == "Duplicate" && child.getPlatformValue() !in blacklist &&
                    child.getPlatformValue() != platform.getOrDefault("None")
                ) {
                    val newLinks = child.changeLog
                        .filter { item -> isLinkToIssue(item, expectedDuplicateText) }
                        .filter { item -> isRecent(item, lastRun) }
                    if (newLinks.isNotEmpty()) {
                        return@fx true
                    }
                }
            }
        false
    }

    private fun isDuplicatedLink(link: Link): Boolean = link.type == "Duplicate" && !link.outwards

    private fun isRecent(change: ChangeLogItem, lastRun: Instant): Boolean = change.created.isAfter(lastRun)

    private fun isLinkToIssue(change: ChangeLogItem, expected: String) =
        change.field == "Link" &&
            change.changedToString?.equals(expected) ?: false

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

    private fun assertPlatformWhitelisted(status: String?, whitelist: List<String>) =
        if ((status.getOrDefault("None")) in whitelist) {
            Unit.right()
        } else {
            OperationNotNeededModuleResponse.left()
        }
}
