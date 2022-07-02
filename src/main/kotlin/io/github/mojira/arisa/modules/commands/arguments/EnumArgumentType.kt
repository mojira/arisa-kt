package io.github.mojira.arisa.modules.commands.arguments

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.concurrent.CompletableFuture

inline fun <reified E : Enum<E>> enumArgumentType() = EnumArgumentType(E::class.java)

private val UNKNOWN_ENUM_CONSTANT_NAME = DynamicCommandExceptionType {
    LiteralMessage("Unknown enum constant name '$it'")
}

class EnumArgumentType<E : Enum<E>>(enumClass: Class<E>) : ArgumentType<E> {
    private val mapping: Map<String, E>
    init {
        val mapping = mutableMapOf<String, E>()
        enumClass.enumConstants
            .map { it.name.lowercase() to it }
            .forEach {
                val name = it.first
                if (!name.all(StringReader::isAllowedInUnquotedString)) {
                    throw IllegalArgumentException("Unsupported name '$name'")
                }

                val old = mapping.put(name, it.second)
                if (old != null) {
                    throw IllegalArgumentException("Lower cased names of constants '${it.second}' and '$old' clash")
                }
            }
        this.mapping = mapping
    }

    override fun parse(reader: StringReader): E {
        val oldCursor = reader.cursor
        val value = reader.readUnquotedString()
        return mapping[value] ?: run {
            reader.cursor = oldCursor
            throw UNKNOWN_ENUM_CONSTANT_NAME.createWithContext(reader, value)
        }
    }

    override fun <S : Any?> listSuggestions(
        context: CommandContext<S>?,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        mapping.keys
            .filter { it.startsWith(builder.remainingLowerCase) }
            .forEach { builder.suggest(it) }
        return builder.buildFuture()
    }

    override fun getExamples(): MutableCollection<String> {
        return mapping.keys.toMutableList()
    }
}
