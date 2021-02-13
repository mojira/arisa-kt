package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.syntax.function.partially1
import arrow.syntax.function.partially2
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.config.SpamPatternConfig
import java.time.Instant

class RemoveSpamModule(private val patternConfigs: List<SpamPatternConfig>) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val removeSpamComments = comments
                .asSequence()
                .filter(::createdSinceLastRun.partially2(lastRun))
                .filter(::userIsNotVolunteer)
                .filter(::isNotStaffRestricted)
                .filter(::isSpam)
                .map { ::restrictComment.partially1(it) }
                .toList()

            assertNotEmpty(removeSpamComments).bind()
            removeSpamComments.forEach { it.invoke() }
        }
    }

    private fun createdSinceLastRun(comment: Comment, lastRun: Instant) =
        comment.created.isAfter(lastRun)

    private fun userIsNotVolunteer(comment: Comment) =
        comment.getAuthorGroups()?.none { listOf("helper", "global-moderators", "staff").contains(it) } ?: true

    private fun isNotStaffRestricted(comment: Comment) =
        comment.visibilityType != "group" || comment.visibilityValue != "staff"

    private fun isSpam(comment: Comment): Boolean {
        val commentContent = comment.body ?: return false
        return patternConfigs.any {
            it.regex.findAll(commentContent).count() >= it.threshold
        }
    }

    private fun restrictComment(comment: Comment) {
        comment.restrict("${comment.body}\nRemoved by Arisa (RemoveSpamModule)")
    }
}
