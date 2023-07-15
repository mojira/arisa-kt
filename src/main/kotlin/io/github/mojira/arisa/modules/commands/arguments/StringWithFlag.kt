package io.github.mojira.arisa.modules.commands.arguments

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import io.github.mojira.arisa.modules.commands.CommandExceptions

/**
 * Similar to [com.mojang.brigadier.arguments.StringArgumentType.greedyString], except that the string
 * may end with an optional flag separated by a space.
 */
fun greedyStringWithFlag(flag: String): ArgumentType<StringWithFlag> = GreedyStringWithTrailingFlagArgumentType(flag)

private class GreedyStringWithTrailingFlagArgumentType(private val flag: String) : ArgumentType<StringWithFlag> {
    private val flagSuffix = " $flag"

    override fun parse(reader: StringReader): StringWithFlag {
        var text = reader.remaining

        // Throw exception when input consists only of flag, to prevent accidental incorrect usage
        if (text == flag) {
            throw CommandExceptions.GREEDY_STRING_ONLY_FLAG.createWithContext(reader, flag)
        }

        // Consume complete input
        reader.cursor = reader.totalLength

        var isFlagSet = false
        if (text.endsWith(flagSuffix)) {
            text = text.removeSuffix(flagSuffix)
            isFlagSet = true
        }

        return StringWithFlag(text, isFlagSet)
    }
}

data class StringWithFlag(
    val string: String,
    /** `true` if the flag was explicitly provided in the command */
    val isFlagSet: Boolean
)
