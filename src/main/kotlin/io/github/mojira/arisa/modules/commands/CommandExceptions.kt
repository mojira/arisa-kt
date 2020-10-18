package io.github.mojira.arisa.modules.commands

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType

object CommandExceptions {
    val ALREADY_FIXED_IN = DynamicCommandExceptionType {
        LiteralMessage("The ticket was already marked as fixed in $it")
    }

    val ALREADY_RESOLVED = DynamicCommandExceptionType {
        LiteralMessage("The ticket was already resolved as $it")
    }

    val NO_SUCH_VERSION = DynamicCommandExceptionType {
        LiteralMessage("The version $it doesn't exist in this project")
    }

    val VERSION_ALREADY_AFFECTED = DynamicCommandExceptionType {
        LiteralMessage("The version $it was already marked as affected")
    }
}
