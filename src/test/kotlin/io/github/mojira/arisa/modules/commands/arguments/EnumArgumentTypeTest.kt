package io.github.mojira.arisa.modules.commands.arguments

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestion
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe

private enum class MyEnum {
    HELLO,
    WORLD
}

@Suppress("unused")
private enum class EnumWithNameClashes {
    @Suppress("EnumEntryName") // ktlint-disable enum-entry-name-case
    a,
    A
}

class EnumArgumentTypeTest : StringSpec({
    "should provide examples" {
        enumArgumentType<MyEnum>().examples shouldBe listOf("hello", "world")
    }

    "should provide suggestions" {
        val dispatcher = CommandDispatcher<Void?>().apply {
            register(literal<Void?>("test").then(argument("arg", enumArgumentType<MyEnum>())))
        }
        val parseResults = dispatcher.parse("test wo", null)

        @Suppress("BlockingMethodInNonBlockingContext")
        val suggestionTexts = dispatcher.getCompletionSuggestions(parseResults).get().list.map(Suggestion::getText)
        suggestionTexts shouldBe listOf("world")
    }

    "should throw for clashing constant names" {
        val e = shouldThrow<IllegalArgumentException> { enumArgumentType<EnumWithNameClashes>() }
        e.message shouldBe "Lower cased names of constants 'A' and 'a' clash"
    }

    "should parse constants" {
        listOf(
            "hello" to MyEnum.HELLO,
            "world" to MyEnum.WORLD
        ).forAll {
            val stringReader = StringReader(it.first)
            enumArgumentType<MyEnum>().parse(stringReader) shouldBe it.second
            stringReader.remainingLength shouldBe 0
        }
    }

    "should fail parsing for unknown constant" {
        val stringReader = StringReader("unknown")
        val e = shouldThrow<CommandSyntaxException> { enumArgumentType<MyEnum>().parse(stringReader) }
        e.message shouldBe "Unknown enum constant name 'unknown' at position 0: <--[HERE]"
        e.cursor shouldBe 0
        // Cursor should not have been changed
        stringReader.cursor shouldBe 0
    }
})
