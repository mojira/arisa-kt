package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

private val A_SECOND_AGO = RIGHT_NOW.minusSeconds(1)

class LanguageModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when ticket was created before the last run" {
        val module = LanguageModule(
            getLanguage = { mapOf("de" to 1.0).right() }
        )
        val issue = mockIssue(
            created = A_SECOND_AGO,
            summary = "Ich habe einen seltsamen Fehler gefunden",
            description = "Es gibt einen Fehler im Minecraft. Bitte schnell beheben!"
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no description, summary or environment" {
        val module = LanguageModule(
            getLanguage = { emptyMap<String, Double>().right() }
        )
        val issue = mockIssue(
            created = RIGHT_NOW
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when description, summary and environment are empty" {
        val module = LanguageModule(
            getLanguage = { emptyMap<String, Double>().right() }
        )
        val issue = mockIssue(
            created = RIGHT_NOW,
            summary = "",
            description = ""
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when no language matches" {
        val module = LanguageModule(
            getLanguage = { emptyMap<String, Double>().right() }
        )
        val issue = mockIssue(
            created = RIGHT_NOW,
            summary = "",
            description = ""
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the combined text does not exceed the threshold" {
        val module = LanguageModule(
            lengthThreshold = 100,
            getLanguage = { mapOf("de" to 1.0).right() }
        )
        val issue = mockIssue(
            created = RIGHT_NOW,
            summary = "?",
            description = "Villagers can open iron doors"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid if ticket is not in English" {
        val module = LanguageModule(
            getLanguage = { mapOf("de" to 1.0).right() }
        )
        val issue = mockIssue(
            created = RIGHT_NOW,
            summary = "Ich habe einen seltsamen Fehler gefunden",
            description = "Es gibt einen Fehler im Minecraft. Bitte schnell beheben!"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if ticket is in English" {
        val module = LanguageModule(
            getLanguage = { mapOf("en" to 1.0).right() }
        )
        val issue = mockIssue(
            created = RIGHT_NOW,
            summary = "I found a strange bug",
            description = "There is an issue in Minecraft. Please fix it as soon as possible"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if ticket is mostly in English" {
        val module = LanguageModule(
            getLanguage = { mapOf("en" to 0.8).right() }
        )
        val issue = mockIssue(
            created = RIGHT_NOW,
            summary = "Coarse Dirt is translated incorrectly in Russian",
            description = "The translation for Acacia slab in Russian is 'Алмазный блок' instead of 'Каменистая земля'."
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid if ticket is mostly not in English" {
        val module = LanguageModule(
            getLanguage = { mapOf("en" to 0.6, "de" to 0.8).right() }
        )
        val issue = mockIssue(
            created = RIGHT_NOW,
            summary = "Wenn ich ein Minecart auf eine Activator Rail setze, wird der Player aus dem Minecart geworfen",
            description = "Im Creative Mode wirft eine Activator Rail den Player aus dem Minecart, ich dachte, dass die Rail das Minecart boostet."
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should pass the combined text to the API" {
        var isApiExecuted = false

        val module = LanguageModule(
            getLanguage = { it shouldBe "Summary. Description."; isApiExecuted = true; mapOf("en" to 1.0).right() }
        )
        val issue = mockIssue(
            created = RIGHT_NOW,
            summary = "Summary.",
            description = "Description."
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
        isApiExecuted shouldBe true
    }

    "should pass the combined text with punctuations to the API" {
        var isApiExecuted = false

        val module = LanguageModule(
            getLanguage = { it shouldBe "Summary. Description."; isApiExecuted = true; mapOf("en" to 1.0).right() }
        )
        val issue = mockIssue(
            created = RIGHT_NOW,
            summary = "Summary",
            description = "Description"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
        isApiExecuted shouldBe true
    }

    "should pass only the summary to the API when description is null" {
        var isApiExecuted = false

        val module = LanguageModule(
            getLanguage = { it shouldBe "Summary."; isApiExecuted = true; mapOf("en" to 1.0).right() }
        )
        val issue = mockIssue(
            created = RIGHT_NOW,
            summary = "Summary."
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
        isApiExecuted shouldBe true
    }

    "should pass only the description to the API when summary is null" {
        var isApiExecuted = false

        val module = LanguageModule(
            getLanguage = { it shouldBe "Description."; isApiExecuted = true; mapOf("en" to 1.0).right() }
        )
        val issue = mockIssue(
            created = RIGHT_NOW,
            description = "Description."
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
        isApiExecuted shouldBe true
    }

    "should pass only the summary to the API when it contains the description" {
        var isApiExecuted = false

        val module = LanguageModule(
            getLanguage = {
                it shouldBe "pillager doesn’t aim child villager.\\n\\nReproduce:\\n\\n1.Summon pillager."
                isApiExecuted = true
                mapOf("en" to 1.0).right()
            }
        )
        val issue = mockIssue(
            created = RIGHT_NOW,
            summary = "pillager doesn’t aim child villager.\\n\\nReproduce:\\n\\n1.Summon pillager.",
            description = "pillager"
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
        isApiExecuted shouldBe true
    }

    "should pass only the description to the API when it contains the summary" {
        var isApiExecuted = false

        val module = LanguageModule(
            getLanguage = {
                it shouldBe "pillager doesn’t aim child villager.\\n\\nReproduce:\\n\\n1.Summon pillager."
                isApiExecuted = true
                mapOf("en" to 1.0).right()
            }
        )
        val issue = mockIssue(
            created = RIGHT_NOW,
            summary = "Pillager doesn’t aim child villager",
            description = "pillager doesn’t aim child villager.\\n\\nReproduce:\\n\\n1.Summon pillager."
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
        isApiExecuted shouldBe true
    }

    "should return OperationNotNeeded if ticket is private" {
        val module = LanguageModule(
            getLanguage = { mapOf("en" to 0.6, "de" to 0.8).right() }
        )
        val issue = mockIssue(
            securityLevel = "private",
            summary = "Wenn ich ein Minecart auf eine Activator Rail setze, wird der Player aus dem Minecart geworfen",
            description = "Im Creative Mode wirft eine Activator Rail den Player aus dem Minecart, ich dachte, dass die Rail das Minecart boostet."
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if ticket contains only a crash report" {
        val module = LanguageModule(
            getLanguage = { mapOf("en" to 1.0).right() }
        )
        val issue = mockIssue(
            created = RIGHT_NOW,
            summary = "java.lang.IllegalArgumentException: bound must be positive",
            description = """
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
            """.trimIndent()
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    /*"should return FailedModuleResponse when resolving as invalid fails" {
        val module = LanguageModule(
            allowedLanguages = emptyList(),
            getLanguage = { mapOf("en" to 1.0).right() }
        )
        val issue = mockIssue(
            created = RIGHT_NOW,
            summary = "Bonjour",
            description = "",
            resolveAsInvalid = { RuntimeException().left() }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }*/

    "should return FailedModuleResponse when adding comment fails" {
        val module = LanguageModule(
            allowedLanguages = emptyList(),
            getLanguage = { mapOf("en" to 1.0).right() }
        )
        val issue = mockIssue(
            created = RIGHT_NOW,
            summary = "Salut",
            description = "",
            addNotEnglishComment = { RuntimeException().left() }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
