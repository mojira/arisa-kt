package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.right
import arrow.syntax.function.partially1
import arrow.syntax.function.partially2
import com.mojang.brigadier.CommandDispatcher
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.AttachmentUtils
import io.github.mojira.arisa.modules.commands.CommandSource
import io.github.mojira.arisa.modules.commands.getCommandDispatcher
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant

data class Command(val command: String, val source: CommandSource)

private typealias CommandResult = Either<Throwable, Int>

private val log: Logger = LoggerFactory.getLogger("CommandModule")

class CommandModule(
    private val prefix: String,
    private val botUserName: String,
    private val ignoreOwnCommands: Boolean,
    attachmentUtils: AttachmentUtils,
    private val getDispatcher: (String) -> CommandDispatcher<CommandSource> =
        ::getCommandDispatcher.partially2(attachmentUtils)
) : Module {
    /**
     * This is the command dispatcher.
     * It's not initialized initially because it relies on `jiraClient` in `ArisaMain`, which is lateinit too.
     * Therefore it's instead initialized only when this module is initially invoked.
     */
    private lateinit var commandDispatcher: CommandDispatcher<CommandSource>

    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = Either.fx {
        if (!::commandDispatcher.isInitialized) {
            commandDispatcher = getDispatcher(prefix)
        }

        val staffComments = issue.comments
            .filter(::isUpdatedAfterLastRun.partially1(lastRun))
            .filter(::isStaffRestricted)
            .filter(::userIsVolunteer)
        assertNotEmpty(staffComments).bind()

        val commands = staffComments
            .map { comment ->
                comment to extractCommands(issue, comment)
            }
            .filter { it.second.isNotEmpty() }
            .onEach { invocation ->
                val commandResults = invocation.second.associate { command ->
                    val result = executeCommand(command)
                    logCommandExecutionResult(issue, invocation.first, command, result)
                    command.source.line to result
                }

                editInvocationComment(invocation.first, commandResults)
            }
        assertNotEmpty(commands).bind()

        ModuleResponse.right().bind()
    }

    private fun executeCommand(command: Command): CommandResult {
        return runBlocking {
            Either.catch {
                commandDispatcher.execute(command.command, command.source)
            }
        }
    }

    private fun isUpdatedAfterLastRun(lastRun: Instant, comment: Comment) = comment.updated.isAfter(lastRun)

    /**
     * Extracts all commands from a comment.
     */
    private fun extractCommands(issue: Issue, comment: Comment) =
        comment.body?.lines().orEmpty()
            .mapIndexed { lineNr, line -> lineNr to line.trim() }
            .filter { (_, line) -> line.startsWith("${prefix}_") }
            .map { (lineNr, line) ->
                Command(line, CommandSource(issue, comment, lineNr))
            }

    private fun logCommandExecutionResult(
        issue: Issue,
        comment: Comment,
        command: Command,
        commandResult: CommandResult
    ) {
        val issueKey = issue.key
        val commentId = comment.id
        val user = comment.author.name
        val commandStr = command.command
        commandResult.fold(
            { exception ->
                log.error("$issueKey: Failed executing command of comment $commentId for user $user: $commandStr",
                    exception)
            },
            { resultValue ->
                // Include user name to be able to detect command misuse
                log.info("$issueKey: Successfully executed command of comment $commentId for user " +
                    "$user (result=$resultValue): $commandStr")
            }
        )
    }

    /**
     * Edits the given comment (that was used to invoke some commands) with the given results.
     *
     * @param results The line numbers of the commands inside of the comment, mapped to the command's result
     */
    private fun editInvocationComment(comment: Comment, results: Map<Int, CommandResult>) {
        val newBody = comment.body?.lines().orEmpty()
            .mapIndexed { lineNr, line ->
                when (val result = results[lineNr]) {
                    null -> line
                    else -> "\n${ getCommandFeedback(line, result) }\n"
                }
            }
            .joinToString("\n")
            .trim()
            // If there are multiple consecutive empty lines, replace them with a single empty line
            .replace(Regex("""\n\n+"""), "\n\n")

        comment.update(newBody)
    }

    /**
     * Get the command feedback string for a command result.
     */
    private fun getCommandFeedback(command: String, result: CommandResult): String {
        val badge = when (result) {
            is Either.Left -> "(x)"
            is Either.Right -> "(/)"
        }

        val feedback = when (result) {
            // Use exception message, or if not present (e.g. for NullPointerException) the string representation
            is Either.Left -> result.a.message ?: result.a.toString()
            is Either.Right -> "Command was executed successfully, with ${result.b} affected element(s)"
        }

        return """
            |$badge $command
            |→ {{$feedback}} ~??[~$botUserName]??~
        """.trimMargin()
    }

    private fun userIsVolunteer(comment: Comment): Boolean {
        // Ignore comments from the bot itself to prevent accidental infinite recursion and command
        // injection by malicious user
        if (ignoreOwnCommands && comment.author.name == botUserName) {
            return false
        }

        return comment.getAuthorGroups()?.any { it == "helper" || it == "global-moderators" || it == "staff" } ?: false
    }

    private fun isStaffRestricted(comment: Comment) =
        comment.visibilityType == "group" && (comment.visibilityValue == "staff" || comment.visibilityValue == "helper")
}
