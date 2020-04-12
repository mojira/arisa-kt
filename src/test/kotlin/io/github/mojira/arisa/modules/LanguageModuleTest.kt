package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.modules.LanguageModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.io.File

class LanguageModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no description, summary or environment" {
        val module = LanguageModule()
        val request = Request(null, null, { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when description, summary and environment are empty" {
        val module = LanguageModule()
        val request = Request("", "", { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when no language matches" {
        val module = LanguageModule()
        val request = Request("##########", "##########", { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid if ticket is not in English" {
        val module = LanguageModule()
        val request = Request(
            "Ich habe einen seltsamen Fehler gefunden",
            "Es gibt einen Fehler im Minecraft. Bitte schnell beheben!",
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if ticket is in English" {
        val module = LanguageModule()
        val request = Request(
            "I found a strange bug",
            "There is an issue in Minecraft. Please fix it as soon as possible",
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if ticket is mostly in English" {
        val module = LanguageModule()
        val request = Request(
            "Coarse Dirt is translated incorrectly in Russian",
            "The translation for Acacia slab in Russian is 'Алмазный блок' instead of 'Каменистая земля'.",
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid if ticket is mostly not in English" {
        val module = LanguageModule()
        val request = Request(
            "Wenn ich ein Minecart auf eine Activator Rail setze, wird der Player aus dem Minecart geworfen",
            "Im Creative Mode wirft eine Activator Rail den Player aus dem Minecart, ich dachte, dass die Rail das Minecart boostet.",
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if ticket contains only a crash report" {
        val module = LanguageModule()
        val request = Request(
            "java.lang.IllegalArgumentException: bound must be positive",
            """
            java.lang.IllegalArgumentException: bound must be positive
                at java.util.Random.nextInt(Random.java:388)
                at ary.a(SourceFile:194)
                at ary.a(SourceFile:49)
                at arm.a(SourceFile:389)
                at asu.a(SourceFile:86)
                at bgv.a(SourceFile:472)
                at qs.a(SourceFile:174)
                at bfh.a(SourceFile:861)
                at qs.c(SourceFile:104)
                at qs.d(SourceFile:116)
                at aqu.a(SourceFile:276)
                at aqu.f(SourceFile:272)
                at aqu.p(SourceFile:657)
                at awn.b(SourceFile:66)
                at atr.a(SourceFile:437)
                at qt.h(SourceFile:384)
                at qt.c(SourceFile:202)
                at net.minecraft.server.MinecraftServer.z(SourceFile:599)
                at po.z(SourceFile:305)
                at net.minecraft.server.MinecraftServer.y(SourceFile:531)
                at net.minecraft.server.MinecraftServer.run(SourceFile:447)
                at java.lang.Thread.run(Thread.java:748)
            """.trimIndent(),
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should handle sample tickets correctly" {
        val module = LanguageModule()
        val tickets = File(this::class.java.getResource("/modules/language").file).listFiles()

        tickets.forEach {
            val (expectedResult, ticket) = """^(\w+)-([A-Z]+-[0-9]+)\.txt$"""
                .toRegex()
                .matchEntire(it.name)!!
                .destructured

            val lines = it.readLines()

            val request = Request(
                lines[0],
                lines.subList(2, lines.size).joinToString("\n"),
                { Unit.right() },
                { language ->
                    assert(language.isoCode639_1.toString() == expectedResult)
                    Unit.right()
                }
            )

            println("Checking test ticket $ticket...")
            val result = module(request)

            if (expectedResult == "valid") {
                result.shouldBeLeft(OperationNotNeededModuleResponse)
            } else {
                result.shouldBeRight(ModuleResponse)
            }
        }
    }

    "should return FailedModuleResponse when resolving as invalid fails" {
        val module = LanguageModule(emptyList())
        val request = Request("Bonjour", "", { RuntimeException().left() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when adding comment fails" {
        val module = LanguageModule(emptyList())
        val request = Request("Salut", "", { Unit.right() }, { RuntimeException().left() })

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
