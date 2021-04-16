package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import io.github.mojira.arisa.jiraClient
import io.github.mojira.arisa.modules.commands.arguments.LinkList
import io.github.mojira.arisa.modules.commands.arguments.LinkListArgumentType

@Suppress("LongMethod")
fun getCommandDispatcher(
    prefix: String
): CommandDispatcher<CommandSource> {
    val addLinksCommand = AddLinksCommand()
    val addVersionCommand = AddVersionCommand()
    val deleteCommentsCommand = DeleteCommentsCommand()
    val deleteLinksCommand = DeleteLinksCommand()
    val fixCapitalizationCommand = FixCapitalizationCommand()
    val fixedCommand = FixedCommand()
    val listUserActivityCommand = ListUserActivityCommand(
        ::getIssuesFromJql.partially1(jiraClient)
    )
    val purgeAttachmentCommand = PurgeAttachmentCommand()
    val removeUserCommand = RemoveUserCommand(
        ::getIssuesFromJql.partially1(jiraClient),
        {
            when (val issue = getIssue(jiraClient, it)) {
                is Either.Left -> issue
                is Either.Right -> (it to issue.b).right()
            }
        },
        { Thread(it).start() }
    )

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

        val fixCapitalizationCommandNode =
            literal<CommandSource>("${prefix}_FIX_CAPITALIZATION")
                .then(
                    argument<CommandSource, String>("empty", greedyString())
                        .executes {
                            fixCapitalizationCommand(
                                it.source.issue
                            )
                        }
                )

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

        val listUserActivityCommandNode =
            literal<CommandSource>("${prefix}_LIST_USER_ACTIVITY")
                .requires(::sentByModerator)
                .then(
                    argument<CommandSource, String>("username", greedyString())
                        .executes {
                            listUserActivityCommand(
                                it.source.issue,
                                it.getString("username")
                            )
                        }
                )

        val purgeAttachmentCommandNode =
            literal<CommandSource>("${prefix}_PURGE_ATTACHMENT")
                .requires(::sentByModerator)
                .then(
                    argument<CommandSource, String>("username", greedyString())
                        .executes {
                            purgeAttachmentCommand(
                                it.source.issue,
                                it.getString("username"),
                                0,
                                Int.MAX_VALUE
                            )
                        }
                        .then(
                            argument<CommandSource, Int>("minId", integer(0))
                                .executes {
                                    purgeAttachmentCommand(
                                        it.source.issue,
                                        it.getString("username"),
                                        it.getInt("minId"),
                                        Int.MAX_VALUE
                                    )
                                }
                                .then(
                                    argument<CommandSource, Int>("maxId", integer(0))
                                        .executes {
                                            purgeAttachmentCommand(
                                                it.source.issue,
                                                it.getString("username"),
                                                it.getInt("minId"),
                                                it.getInt("maxId")
                                            )
                                        }
                                )
                        )
                )

        val removeUserCommandNode =
            literal<CommandSource>("${prefix}_REMOVE_USER")
                .requires(::sentByModerator)
                .then(
                    argument<CommandSource, String>("username", greedyString())
                        .executes {
                            removeUserCommand(
                                it.source.issue,
                                it.getString("username")
                            )
                        }
                )

        register(addLinksCommandNode)
        register(addVersionCommandNode)
        register(deleteCommentsCommandNode)
        register(deleteLinksCommandNode)
        register(fixCapitalizationCommandNode)
        register(fixedCommandNode)
        register(listUserActivityCommandNode)
        register(purgeAttachmentCommandNode)
        register(removeCommentsCommandNode)
        register(removeLinksCommandNode)
        register(removeUserCommandNode)
    }
}

private fun sentByModerator(source: CommandSource) =
    source.comment.author?.groups.orEmpty().any { it == "global-moderators" || it == "staff" }

private fun CommandContext<*>.getInt(name: String) = getArgument(name, Int::class.java)
private fun CommandContext<*>.getLinkList(name: String) = getArgument(name, LinkList::class.java)
private fun CommandContext<*>.getString(name: String) = getArgument(name, String::class.java)
