package io.github.mojira.arisa.modules.commands

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType

object CommandExceptions {
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

    val FIX_VERSION_BEFORE_FIRST_AFFECTED_VERSION = DynamicCommandExceptionType {
        LiteralMessage("Cannot add fix version $it because the first affected " +
                "version of the issue was released after it")
    }

    val INVALID_LINK_TYPE = SimpleCommandExceptionType(
        LiteralMessage("Cannot parse a valid link type")
    )

    val INVALID_TICKET_KEY = SimpleCommandExceptionType(
        LiteralMessage("Found invalid ticket key")
    )

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
}
