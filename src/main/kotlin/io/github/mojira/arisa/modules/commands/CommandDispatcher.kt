package io.github.mojira.arisa.modules.commands

import arrow.core.Either
import arrow.core.right
import arrow.syntax.function.partially1
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.arguments.StringArgumentType.string
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import io.github.mojira.arisa.infrastructure.AttachmentUtils
import io.github.mojira.arisa.infrastructure.jira.getIssue
import io.github.mojira.arisa.infrastructure.jira.getIssuesFromJql
import io.github.mojira.arisa.jiraClient
import io.github.mojira.arisa.modules.commands.arguments.LinkList
import io.github.mojira.arisa.modules.commands.arguments.LinkListArgumentType
import io.github.mojira.arisa.modules.commands.arguments.StringWithFlag
import io.github.mojira.arisa.modules.commands.arguments.enumArgumentType
import io.github.mojira.arisa.modules.commands.arguments.greedyStringWithFlag

@Suppress("LongMethod")
fun getCommandDispatcher(
    prefix: String,
    attachmentUtils: AttachmentUtils
): CommandDispatcher<CommandSource> {
    val addLinksCommand = AddLinksCommand()
    val addVersionCommand = AddVersionCommand()
    val clearProjectCacheCommand = ClearProjectCacheCommand()
    val deleteCommentsCommand = DeleteCommentsCommand()
    val deleteLinksCommand = DeleteLinksCommand()
    val deobfuscateCommand = DeobfuscateCommand(attachmentUtils)
    val fixCapitalizationCommand = FixCapitalizationCommand()
    val fixedCommand = FixedCommand()
    val listUserActivityCommand = ListUserActivityCommand(
        ::getIssuesFromJql.partially1(jiraClient)
    )
    val makePrivateCommand = MakePrivateCommand()
    val purgeAttachmentCommand = PurgeAttachmentCommand()
    val reopenCommand = ReopenCommand()
    val removeContentCommand = RemoveContentCommand(
        ::getIssuesFromJql.partially1(jiraClient),
        {
            when (val issue = getIssue(jiraClient, it)) {
                is Either.Left -> issue
                is Either.Right -> (it to issue.b).right()
            }
        },
        { Thread(it).start() }
    )
    val shadowbanCommand = ShadowbanCommand()

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

        val clearProjectCacheCommandNode =
            literal<CommandSource>("${prefix}_CLEAR_PROJECT_CACHE")
                .executes {
                    clearProjectCacheCommand()
                }

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

        val deobfuscateCommandNode = run {
            val attachmentIdArg = "attachmentId"
            val minecraftVersionIdArg = "minecraftVersionId"
            val crashReportTypeArg = "crashReportType"
            literal<CommandSource>("${prefix}_DEOBFUSCATE")
                .then(
                    argument<CommandSource, String>(attachmentIdArg, string())
                        .executes {
                            deobfuscateCommand.invoke(
                                it.source.issue,
                                it.getString(attachmentIdArg)
                            )
                        }
                        .then(
                            argument<CommandSource, String>(minecraftVersionIdArg, string())
                                .executes {
                                    deobfuscateCommand.invoke(
                                        it.source.issue,
                                        it.getString(attachmentIdArg),
                                        it.getString(minecraftVersionIdArg)
                                    )
                                }
                                .then(
                                    argument<CommandSource, CrashReportType>(
                                        crashReportTypeArg,
                                        enumArgumentType<CrashReportType>()
                                    )
                                        .executes {
                                            deobfuscateCommand.invoke(
                                                it.source.issue,
                                                it.getString(attachmentIdArg),
                                                it.getString(minecraftVersionIdArg),
                                                it.getArgument(crashReportTypeArg, CrashReportType::class.java)
                                            )
                                        }
                                )
                        )
                )
        }

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
                .executes {
                    fixCapitalizationCommand(
                        it.source.issue
                    )
                }

        val fixedCommandNode =
            literal<CommandSource>("${prefix}_FIXED")
                .requires(::sentByModerator)
                .then(
                    argument<CommandSource, StringWithFlag>("version", greedyStringWithFlag("force"))
                        .executes {
                            val (version, force) = it.getStringWithFlag("version")
                            fixedCommand(
                                it.source.issue,
                                version,
                                force
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

        val makePrivateCommandNode =
            literal<CommandSource>("${prefix}_MAKE_PRIVATE")
                .executes {
                    makePrivateCommand(
                        it.source.issue
                    )
                }

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

        val removeContentCommandNode =
            literal<CommandSource>("${prefix}_REMOVE_CONTENT")
                .requires(::sentByModerator)
                .then(
                    argument<CommandSource, String>("username", greedyString())
                        .executes {
                            removeContentCommand(
                                it.source.issue,
                                it.getString("username")
                            )
                        }
                )

        val reopenCommandNode =
            literal<CommandSource>("${prefix}_REOPEN")
                .executes {
                    reopenCommand(
                        it.source.issue
                    )
                }

        val shadowbanCommandNode =
            literal<CommandSource>("${prefix}_SHADOWBAN")
                .requires(::sentByModerator)
                .then(
                    argument<CommandSource, String>("username", greedyString())
                        .executes {
                            shadowbanCommand(
                                it.getString("username")
                            )
                        }
                )

        register(addLinksCommandNode)
        register(addVersionCommandNode)
        register(clearProjectCacheCommandNode)
        register(deleteCommentsCommandNode)
        register(deleteLinksCommandNode)
        register(deobfuscateCommandNode)
        register(fixCapitalizationCommandNode)
        register(fixedCommandNode)
        register(listUserActivityCommandNode)
        register(makePrivateCommandNode)
        register(purgeAttachmentCommandNode)
        register(removeCommentsCommandNode)
        register(removeLinksCommandNode)
        register(removeContentCommandNode)
        register(reopenCommandNode)
        register(shadowbanCommandNode)
    }
}

private fun sentByModerator(source: CommandSource) =
    source.comment.getAuthorGroups()?.any { it == "global-moderators" || it == "staff" } ?: false

private fun CommandContext<*>.getInt(name: String) = getArgument(name, Int::class.java)
private fun CommandContext<*>.getLinkList(name: String) = getArgument(name, LinkList::class.java)
private fun CommandContext<*>.getString(name: String) = getArgument(name, String::class.java)
private fun CommandContext<*>.getStringWithFlag(name: String) = getArgument(name, StringWithFlag::class.java)
