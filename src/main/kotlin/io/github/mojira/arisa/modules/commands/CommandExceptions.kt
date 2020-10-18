package io.github.mojira.arisa.modules.commands

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType

object CommandExceptions {
    val ALREADY_FIXED_IN = DynamicCommandExceptionType {
        LiteralMessage("The ticket was already marked as fixed in $it")
    }

    val ALREADY_RESOLVED = DynamicCommandExceptionType {
        LiteralMessage("The ticket was already resolved as $it")
    }

    val INVALID_TICKET_KEY = DynamicCommandExceptionType {
        LiteralMessage("Found invalid ticket key in ${(it as List<*>).joinToString()}")
    }

    val LEFT_EITHER = DynamicCommandExceptionType {
        LiteralMessage("Something went wrong, but I'm too lazy to interpret the detail (>ω<): $it")
    }

    val NO_SUCH_VERSION = DynamicCommandExceptionType {
        LiteralMessage("The version $it doesn't exist in this project")
    }

    val UNKNOWN_LINK_TYPE = DynamicCommandExceptionType {
        LiteralMessage("Cannot parse a valid link type from $it")
    }

    val VERSION_ALREADY_AFFECTED = DynamicCommandExceptionType {
        LiteralMessage("The version $it was already marked as affected")
    }
}
