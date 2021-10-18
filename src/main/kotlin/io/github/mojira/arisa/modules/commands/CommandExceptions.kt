package io.github.mojira.arisa.modules.commands

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType

object CommandExceptions {
    // Most exceptions here are not actually command syntax related, but Brigadier currently has no
    // exception for command execution failure, see https://github.com/Mojang/brigadier/issues/100
    class CommandExecutionException : Exception {
        constructor(message: String?) : super(message)
        constructor(message: String?, cause: Throwable?) : super(message, cause)
    }

    val ALREADY_FIXED_IN = DynamicCommandExceptionType {
        LiteralMessage("The ticket was already marked as fixed in $it")
    }

    val ALREADY_PRIVATE = SimpleCommandExceptionType(
        LiteralMessage("The ticket already had a security level set")
    )

    val ALREADY_RESOLVED = DynamicCommandExceptionType {
        LiteralMessage("The ticket was already resolved as $it")
    }

    val CANNOT_QUERY_USER_ACTIVITY = DynamicCommandExceptionType {
        LiteralMessage("Could not query activity of user \"$it\"")
    }

    val FIX_VERSION_SAME_OR_BEFORE_AFFECTED_VERSION = Dynamic2CommandExceptionType {
        fixVersionName, affectedVersionName -> LiteralMessage("Cannot add fix version $fixVersionName " +
            "because the affected version $affectedVersionName of the issue is the same or was released after " +
            "it; run with `<version> force` to add the fix version anyways")
    }

    val INVALID_LINK_TYPE = SimpleCommandExceptionType(
        LiteralMessage("Cannot parse a valid link type")
    )

    val INVALID_TICKET_KEY = SimpleCommandExceptionType(
        LiteralMessage("Found invalid ticket key")
    )

    val GREEDY_STRING_ONLY_FLAG = DynamicCommandExceptionType {
        LiteralMessage("Argument consists only of flag '$it' but does not contain a string")
    }

    val LEFT_EITHER = DynamicCommandExceptionType {
        LiteralMessage("Something went wrong, but I'm too lazy to interpret the details for you (>ω<): $it")
    }

    val NO_CAPITALIZATION_MATCHES = SimpleCommandExceptionType(
        LiteralMessage("No incorrect capitalization matches were found")
    )

    val NO_SUCH_VERSION = DynamicCommandExceptionType {
        LiteralMessage("The version $it doesn't exist in this project")
    }

    val NOT_AR = SimpleCommandExceptionType(
        LiteralMessage("The ticket was not resolved as Awaiting Response")
    )

    val TEST_EXCEPTION = DynamicCommandExceptionType {
        LiteralMessage("Testing error message: $it")
    }

    val VERSION_ALREADY_AFFECTED = DynamicCommandExceptionType {
        LiteralMessage("The version $it was already marked as affected")
    }

    val NO_ATTACHMENT_WITH_ID = DynamicCommandExceptionType {
        LiteralMessage("Attachment with ID '$it' does not exist")
    }

    val ATTACHMENT_ALREADY_EXISTS = DynamicCommandExceptionType {
        LiteralMessage("Attachment with name '$it' already exists")
    }
}
