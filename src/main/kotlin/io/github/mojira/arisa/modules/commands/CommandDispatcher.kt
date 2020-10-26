package io.github.mojira.arisa.modules.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.modules.commands.arguments.LinkList
import io.github.mojira.arisa.modules.commands.arguments.LinkListArgumentType

data class CommandSource(
    val issue: Issue,
    val comment: Comment
)

fun getCommandDispatcher(
    prefix: String,
    addLinksCommand: Command<LinkList> = AddLinksCommand(),
    addVersionCommand: Command<String> = AddVersionCommand(),
    deleteCommentsCommand: Command<String> = DeleteCommentsCommand(),
    deleteLinksCommand: Command<LinkList> = DeleteLinksCommand(),
    fixedCommand: Command<String> = FixedCommand(),
    purgeAttachmentCommand: Command2<Int, Int> = PurgeAttachmentCommand()
): CommandDispatcher<CommandSource> {
    return CommandDispatcher<CommandSource>().apply {
        val addLinksCommandNode =
            literal<CommandSource>("${prefix}_ADD_LINKS")
                .requires(::sentByModerator)
                .then(
                    argument<CommandSource, LinkList>("linkList", LinkListArgumentType())
                        .executes {
                            addLinksCommand(
                                it.source.issue,
                                it.getLinkList("linkList")
                            )
                        }
                )

        val addVersionCommandNode =
            literal<CommandSource>("${prefix}_ADD_VERSION")
                .then(
                    argument<CommandSource, String>("version", greedyString())
                        .executes {
                            addVersionCommand(
                                it.source.issue,
                                it.getString("version")
                            )
                        }
                )

        val deleteCommentsCommandNodeChild =
            argument<CommandSource, String>("name", greedyString())
                .executes {
                    deleteCommentsCommand(
                        it.source.issue,
                        it.getString("name")
                    )
                }
        val deleteCommentsCommandNode =
            literal<CommandSource>("${prefix}_DELETE_COMMENTS")
                .requires(::sentByModerator)
                .then(deleteCommentsCommandNodeChild)
        val removeCommentsCommandNode =
            literal<CommandSource>("${prefix}_REMOVE_COMMENTS")
                .requires(::sentByModerator)
                .then(deleteCommentsCommandNodeChild)

        val deleteLinksCommandNodeChild =
            argument<CommandSource, LinkList>("linkList", LinkListArgumentType())
                .executes {
                    deleteLinksCommand(
                        it.source.issue,
                        it.getLinkList("linkList")
                    )
                }
        val deleteLinksCommandNode =
            literal<CommandSource>("${prefix}_DELETE_LINKS")
                .requires(::sentByModerator)
                .then(deleteLinksCommandNodeChild)
        val removeLinksCommandNode =
            literal<CommandSource>("${prefix}_REMOVE_LINKS")
                .requires(::sentByModerator)
                .then(deleteLinksCommandNodeChild)

        val fixedCommandNode =
            literal<CommandSource>("${prefix}_FIXED")
                .requires(::sentByModerator)
                .then(
                    argument<CommandSource, String>("version", greedyString())
                        .executes {
                            fixedCommand(
                                it.source.issue,
                                it.getString("version")
                            )
                        }
                )

        val purgeAttachmentCommandNode =
            literal<CommandSource>("${prefix}_PURGE_ATTACHMENT")
                .requires(::sentByModerator)
                .then(
                    argument<CommandSource, Int>("start", integer(0))
                        .executes {
                            purgeAttachmentCommand(
                                it.source.issue,
                                it.getInt("start"),
                                Int.MAX_VALUE
                            )
                        }
                        .then(
                            argument<CommandSource, Int>("end", integer(0))
                                .executes {
                                    purgeAttachmentCommand(
                                        it.source.issue,
                                        it.getInt("start"),
                                        it.getInt("end")
                                    )
                                }
                        )
                )

        register(addLinksCommandNode)
        register(addVersionCommandNode)
        register(deleteCommentsCommandNode)
        register(deleteLinksCommandNode)
        register(fixedCommandNode)
        register(purgeAttachmentCommandNode)
        register(removeCommentsCommandNode)
        register(removeLinksCommandNode)
    }
}

private fun sentByModerator(source: CommandSource) =
    source.comment.getAuthorGroups()?.any { it == "global-moderators" || it == "staff" } ?: false

private fun CommandContext<*>.getInt(name: String) = getArgument(name, Int::class.java)
private fun CommandContext<*>.getLinkList(name: String) = getArgument(name, LinkList::class.java)
private fun CommandContext<*>.getString(name: String) = getArgument(name, String::class.java)
