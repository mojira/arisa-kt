package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially1
import com.mojang.brigadier.CommandDispatcher
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.commands.CommandSource
import io.github.mojira.arisa.modules.commands.getCommandDispatcher
import kotlinx.coroutines.runBlocking
import java.time.Instant

class CommandModule(
    private val prefix: String,
    private val getDispatcher: (String) -> CommandDispatcher<CommandSource> = ::getCommandDispatcher
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

        with(issue) {
            val staffComments = comments
                .filter(::isUpdatedAfterLastRun.partially1(lastRun))
                .filter(::isStaffRestricted)
                .filter(::userIsVolunteer)
                .filter(::isProbablyACommand)
            assertNotEmpty(staffComments).bind()

            val results = staffComments
                .map { it to executeCommand(it, this) }

            if (results.isEmpty()) {
                (OperationNotNeededModuleResponse.left() as Either<ModuleError, ModuleResponse>).bind()
            }
            results.forEach { (comment, result) ->
                if (result.isLeft()) {
                    comment.update("[~arisabot]: (x) ${(result as Either.Left).a.message}.\n----\n${comment.body}")
                } else {
                    comment.update("[~arisabot]: (/) ${(result as Either.Right).b}.\n----\n${comment.body}")
                }
            }
            ModuleResponse.right().bind()
        }
    }

    @Suppress("SpreadOperator")
    private fun executeCommand(comment: Comment, issue: Issue): Either<Throwable, Int> {
        val source = CommandSource(issue, comment)
        return runBlocking {
            Either.catch {
                commandDispatcher.execute(comment.body, source)
            }
        }
    }

    private fun isUpdatedAfterLastRun(lastRun: Instant, comment: Comment) = comment.updated.isAfter(lastRun)

    private fun isProbablyACommand(comment: Comment) =
        !comment.body.isNullOrBlank() &&
                comment.body.startsWith("${prefix}_") &&
                (comment.body.count { it.isWhitespace() } > 0)

    private fun userIsVolunteer(comment: Comment) =
        comment.getAuthorGroups()?.any { it == "helper" || it == "global-moderators" || it == "staff" } ?: false

    private fun isStaffRestricted(comment: Comment) =
        comment.visibilityType == "group" && (comment.visibilityValue == "staff" || comment.visibilityValue == "helper")
}
