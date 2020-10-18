package io.github.mojira.arisa.modules.commands

import arrow.syntax.function.partially1
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType.getInteger
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue

data class CommandSource(
    val issue: Issue,
    val comment: Comment
)

val COMMAND_DISPATCHER = CommandDispatcher<CommandSource>().apply {
    register(addLinksCommandNode)
    register(addVersionCommandNode)
    register(deleteCommentsCommandNode)
    register(fixedCommandNode)
    register(purgeAttachmentCommandNode)
}

private val addLinksCommandNode =
    literal<CommandSource>("ARISA_ADD_LINKS")
        .requires(::sentByModerator)
        .then(
            // Holly crap.
        )

private val addVersionCommand = AddVersionCommand()
private val addVersionCommandNode =
    literal<CommandSource>("ARISA_ADD_VERSION")
        .then(
            argument<CommandSource, String>("version", greedyString())
                .executes {
                    addVersionCommand.invoke(it.source.issue, getString(it, "version"))
                }
        )

private val deleteCommentsCommand = DeleteCommentsCommand()
private val deleteCommentsCommandNode =
    literal<CommandSource>("ARISA_REMOVE_COMMENTS")
        .requires(::sentByModerator)
        .then(
            argument<CommandSource, String>("name", greedyString())
                .executes {
                    deleteCommentsCommand.invoke(it.source.issue, getString(it, "name"))
                }
        )

private val fixedCommand = FixedCommand()
private val fixedCommandNode =
    literal<CommandSource>("ARISA_FIXED")
        .requires(::sentByModerator)
        .then(
            argument<CommandSource, String>("version", greedyString())
                .executes {
                    fixedCommand.invoke(it.source.issue, getString(it, "version"))
                }
        )

private val purgeAttachmentComment = PurgeAttachmentCommand()
private val purgeAttachmentCommandNode =
    literal<CommandSource>("ARISA_PURGE_ATTACHMENT")
        .requires(::sentByModerator)
        .then(
            argument<CommandSource, Int>("start", integer(0))
                .executes {
                    purgeAttachmentComment.invoke(
                        it.source.issue,
                        getInteger(it, "start")
                    )
                }
                .then(
                    argument<CommandSource, Int>("end", integer(0))
                        .executes {
                            purgeAttachmentComment.invoke(
                                it.source.issue,
                                getInteger(it, "start"),
                                getInteger(it, "end")
                            )
                        }
                )
        )

private fun sentByModerator(source: CommandSource) =
    source.comment.getAuthorGroups()?.any { it == "global-moderators" || it == "staff" } ?: false
