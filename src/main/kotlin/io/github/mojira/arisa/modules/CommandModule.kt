package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.commands.AddVersionCommand
import io.github.mojira.arisa.modules.commands.Command
import io.github.mojira.arisa.modules.commands.FixedCommand
import java.time.Instant

// TODO if we get a lot of commands it might make sense to create a command registry
class CommandModule(
    val addVersionCommand: Command = AddVersionCommand(),
    val fixedCommand: Command = FixedCommand()
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = Either.fx {
        with(issue) {
            val staffComments = comments
                .filter(::isStaffRestricted)
                .filter(::userIsVolunteer)
                .filter(::isProbablyACommand)
            assertNotEmpty(staffComments).bind()

            val results = staffComments
                .map { executeCommand(it.body!!, this) }

            when {
                results.any { it.isLeft() && (it as Either.Left).a is FailedModuleResponse } ->
                    results.first { (it as Either.Left).a is FailedModuleResponse }.bind()
                results.any { it.isRight() } -> results.first { it.isRight() }.bind()
                else -> OperationNotNeededModuleResponse.left().bind()
            }
        }
    }

    @Suppress("SpreadOperator")
    private fun executeCommand(comment: String, issue: Issue): Either<ModuleError, ModuleResponse> {
        val split = comment.split("\\s+".toRegex())
        val arguments = split.toTypedArray()
        return when (split[0]) {
            // TODO this should be configurable if we move to a registry
            // TODO do we want to add the response of a module via editing the comment?
            "ARISA_ADD_VERSION" -> addVersionCommand(issue, *arguments)
            "ARISA_FIXED" -> fixedCommand(issue, *arguments)
            else -> OperationNotNeededModuleResponse.left()
        }
    }

    private fun isProbablyACommand(comment: Comment) =
        !comment.body.isNullOrBlank() &&
                comment.body.startsWith("ARISA_") &&
                (comment.body.count { it.isWhitespace() } > 0)

    private fun userIsVolunteer(comment: Comment) =
        comment.getAuthorGroups()?.any { it == "helper" || it == "global-moderators" || it == "staff" } ?: false

    private fun isStaffRestricted(comment: Comment) =
        comment.visibilityType == "group" && (comment.visibilityValue == "staff" || comment.visibilityValue == "helper")
}
