package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant

class RemoveBotCommentModule(
    private val botUserName: String,
    private val removalTag: String
) : Module {
    private val log: Logger = LoggerFactory.getLogger("RemoveBotCommentModule")

    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {

            // Only consider new comments
            val newComments = comments.filter {
                it.updated.isAfter(lastRun)
            }

            var performedRemoval = false
            newComments.forEach {
                if (shouldBeRemoved(it)) {
                    it.remove()
                    log.debug("Removed bot comment [${it.id}] from [${issue.key}]")
                    performedRemoval = true
                }
            }
            assertTrue(performedRemoval).bind()
        }
    }

    private fun isVolunteerRestricted(comment: Comment) =
        comment.visibilityType == "group" &&
            listOf("staff", "global-moderators").contains(comment.visibilityValue)

    private fun shouldBeRemoved(comment: Comment) =
        comment.author.name == botUserName &&
            isVolunteerRestricted(comment) &&
            comment.body.equals(removalTag)
}
