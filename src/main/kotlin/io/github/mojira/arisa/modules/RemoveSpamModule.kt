package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.partially1
import arrow.syntax.function.partially2
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.config.SpamPatternConfig
import java.time.Instant

@Suppress("TooManyFunctions")
class RemoveSpamModule(private val patternConfigs: List<SpamPatternConfig>) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val actions = removeSpamComments(comments, lastRun) + checkIssue(issue, lastRun)

            assertNotEmpty(actions).bind()
            actions.forEach { it.invoke() }
        }
    }

    private fun removeSpamComments(comments: List<Comment>, lastRun: Instant): List<() -> Unit> =
        comments
            .asSequence()
            .filter(::createdSinceLastRun.partially2(lastRun))
            .filter(::userIsNotVolunteer)
            .filter(::isNotStaffRestricted)
            .filter(::isCommentSpam)
            .map { ::restrictComment.partially1(it) }
            .toList()

    private fun createdSinceLastRun(comment: Comment, lastRun: Instant) =
        comment.created.isAfter(lastRun)

    private fun userIsNotVolunteer(comment: Comment) =
        comment.getAuthorGroups()?.none { listOf("helper", "global-moderators", "staff").contains(it) } ?: true

    private fun isNotStaffRestricted(comment: Comment) =
        comment.visibilityType != "group" || comment.visibilityValue != "staff"

    private fun isCommentSpam(comment: Comment): Boolean {
        return isTextSpam(comment.body ?: return false)
    }

    private fun isTextSpam(text: String): Boolean =
        patternConfigs.any {
            it.regex.findAll(text).count() >= it.threshold
        }

    private fun restrictComment(comment: Comment) {
        comment.restrict("${comment.body}\nRemoved by Arisa (RemoveSpamModule)")
    }

    private fun checkIssue(issue: Issue, lastRun: Instant): List<() -> Unit> {
        val shouldTakeAction = isBugreportNew(issue, lastRun) && isBugreportSpam(issue)

        return if (shouldTakeAction)
            listOf {
                issue.putInSpamBin()
            }
        else
            emptyList()
    }

    private fun isBugreportNew(issue: Issue, lastRun: Instant) =
        issue.created.isAfter(lastRun)

    private fun isBugreportSpam(issue: Issue): Boolean {
        val reporter = issue.reporter ?: return false
        val groups = reporter.getGroups() ?: listOf()
        val userIsVolunteer = groups.any { listOf("helper", "global-moderators", "staff").contains(it) }

        return if (userIsVolunteer) false
        else isTextSpam(issue.summary ?: "") ||
            isTextSpam(issue.description ?: "") ||
            isTextSpam(issue.environment ?: "")
    }

    private fun Issue.putInSpamBin() {
        changeReporter("SpamBin")
        if (securityLevel == null) setPrivate()
        if (resolution == null || resolution == "Unresolved") resolveAsInvalid()
    }
}
