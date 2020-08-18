package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.syntax.function.partially1
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.commands.AddVersionCommand
import io.github.mojira.arisa.modules.commands.Command
import io.github.mojira.arisa.modules.commands.DeleteCommentsCommand
import io.github.mojira.arisa.modules.commands.FixedCommand
import io.github.mojira.arisa.modules.commands.PurgeAttachmentCommand
import java.time.Instant

// TODO if we get a lot of commands it might make sense to create a command registry
class CommandModule(
    val addVersionCommand: Command = AddVersionCommand(),
    val fixedCommand: Command = FixedCommand(),
    val purgeAttachmentCommand: Command = PurgeAttachmentCommand(),
    val deleteCommentsCommand: Command = DeleteCommentsCommand()
) : Module {
    private lateinit var staffAuthor: User
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = Either.fx {
        with(issue) {
            val staffComments = comments
                .filter(::isUpdatedAfterLastRun.partially1(lastRun))
                .filter(::isStaffRestricted)
                .filter(::userIsVolunteer)
                .filter(::isProbablyACommand)
            assertNotEmpty(staffComments).bind()

            val results = staffComments
                .map { executeCommand(it.body!!, this, userIsMod(it), it.author) }

            when {
                results.any { it.isLeft() && (it as Either.Left).a is FailedModuleResponse } -> {
                    addRawRestrictedComment("$staffAuthor Command execution failed", "helper")
                    results.first { (it as Either.Left).a is FailedModuleResponse }.bind()
                }
                results.any { it.isRight() } -> {
                    addRawRestrictedComment("$staffAuthor Command execution was successful", "helper")
                    results.first { it.isRight() }.bind()
                }
                else -> OperationNotNeededModuleResponse.left().bind()
            }
        }
    }

    @Suppress("SpreadOperator")
    private fun executeCommand(comment: String, issue: Issue, userIsMod: Boolean, author: User): Either<ModuleError, ModuleResponse> {
        val split = comment.split("\\s+".toRegex())
        val arguments = split.toTypedArray()
        staffAuthor = author
        return when (split[0]) {
            // TODO this should be configurable if we move to a registry
            // TODO do we want to add the response of a module via editing the comment?
            "ARISA_ADD_VERSION" -> addVersionCommand(issue, *arguments)
            "ARISA_FIXED" -> if (userIsMod) {
                fixedCommand(issue, *arguments)
            } else OperationNotNeededModuleResponse.left()
            "ARISA_PURGE_ATTACHMENT" -> if (userIsMod) {
                purgeAttachmentCommand(issue, *arguments)
            } else OperationNotNeededModuleResponse.left()
            "ARISA_REMOVE_COMMENTS" -> if (userIsMod) {
                deleteCommentsCommand(issue, *arguments)
            } else OperationNotNeededModuleResponse.left()
            else -> OperationNotNeededModuleResponse.left()
        }
    }

    private fun isUpdatedAfterLastRun(lastRun: Instant, comment: Comment) = comment.updated.isAfter(lastRun)

    private fun isProbablyACommand(comment: Comment) =
        !comment.body.isNullOrBlank() &&
                comment.body.startsWith("ARISA_") &&
                (comment.body.count { it.isWhitespace() } > 0)

    private fun userIsVolunteer(comment: Comment) =
        comment.getAuthorGroups()?.any { it == "helper" || it == "global-moderators" || it == "staff" } ?: false

    private fun userIsMod(comment: Comment) =
        comment.getAuthorGroups()?.any { it == "global-moderators" || it == "staff" } ?: false

    private fun isStaffRestricted(comment: Comment) =
        comment.visibilityType == "group" && (comment.visibilityValue == "staff" || comment.visibilityValue == "helper")
}
