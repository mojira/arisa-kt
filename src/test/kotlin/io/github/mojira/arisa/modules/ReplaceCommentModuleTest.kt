package io.github.mojira.arisa.modules

import arrow.core.right
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReplaceCommentModuleTest : StringSpec({
    val module = ReplaceCommentModule()
    "should return OperationNotNeededModuleResponse when there is no comment" {
        val request = ReplaceCommentModule.Request(
            42L,
            listOf()
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
    "should return OperationNotNeededModuleResponse when the comment is updated before last run" {
        val request = ReplaceCommentModule.Request(
            42L,
            listOf(
                ReplaceCommentModule.Comment(1L, "https://bugs.mojang.com/browse/MC-1") { Unit.right() }
            )
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
    "should return OperationNotNeededModuleResponse when the comment doesn't need replace" {
        val request = ReplaceCommentModule.Request(
            42L,
            listOf(
                ReplaceCommentModule.Comment(100L, "MC-1") { Unit.right() }
            )
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
    "should return OperationNotNeededModuleResponse when the title of the link is not a ticket ID" {
        val request = ReplaceCommentModule.Request(
            42L,
            listOf(
                ReplaceCommentModule.Comment(100L, "[A similar issue with this was fixed previously|https://bugs.mojang.com/browse/MC-4]") { Unit.right() }
            )
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
    "should return OperationNotNeededModuleResponse when the title of the link is not the same ticket as specified in the /browse link" {
        val request = ReplaceCommentModule.Request(
            42L,
            listOf(
                ReplaceCommentModule.Comment(100L, "[MC-5|https://bugs.mojang.com/browse/MC-4]") { Unit.right() }
            )
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
    "should return OperationNotNeededModuleResponse when the title of the link is not the same ticket as specified in the /projects link" {
        val request = ReplaceCommentModule.Request(
            42L,
            listOf(
                ReplaceCommentModule.Comment(100L, "[MC-5|https://bugs.mojang.com/projects/MC/issues/MC-4]") { Unit.right() }
            )
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
    "should replace /browse links" {
        var replacedCommentBody: String? = null

        val request = ReplaceCommentModule.Request(
            42L,
            listOf(
                ReplaceCommentModule.Comment(100L, "Duplicates https://bugs.mojang.com/browse/MC-4") {
                    replacedCommentBody = it
                    Unit.right()
                }
            )
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedCommentBody shouldBe "Duplicates MC-4"
    }
    "should replace /projects links" {
        var replacedCommentBody: String? = null

        val request = ReplaceCommentModule.Request(
            42L,
            listOf(
                ReplaceCommentModule.Comment(100L, "Duplicates https://bugs.mojang.com/projects/MC/issues/MC-4") {
                    replacedCommentBody = it
                    Unit.right()
                }
            )
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedCommentBody shouldBe "Duplicates MC-4"
    }
    "should replace /browse links with query" {
        var replacedCommentBody: String? = null

        val request = ReplaceCommentModule.Request(
            42L,
            listOf(
                ReplaceCommentModule.Comment(100L, "Duplicates https://bugs.mojang.com/browse/MC-4?jql=votes%3E0") {
                    replacedCommentBody = it
                    Unit.right()
                }
            )
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedCommentBody shouldBe "Duplicates MC-4"
    }
    "should replace /projects links with query" {
        var replacedCommentBody: String? = null

        val request = ReplaceCommentModule.Request(
            42L,
            listOf(
                ReplaceCommentModule.Comment(100L, "Duplicates https://bugs.mojang.com/projects/MC/issues/MC-4?jql=votes%3E0") {
                    replacedCommentBody = it
                    Unit.right()
                }
            )
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedCommentBody shouldBe "Duplicates MC-4"
    }
    "should replace titled /browse links" {
        var replacedCommentBody: String? = null

        val request = ReplaceCommentModule.Request(
            42L,
            listOf(
                ReplaceCommentModule.Comment(100L, "Duplicates [MC-4|https://bugs.mojang.com/browse/MC-4]") {
                    replacedCommentBody = it
                    Unit.right()
                }
            )
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedCommentBody shouldBe "Duplicates MC-4"
    }
    "should replace titled /projects links" {
        var replacedCommentBody: String? = null

        val request = ReplaceCommentModule.Request(
            42L,
            listOf(
                ReplaceCommentModule.Comment(100L, "Duplicates [MC-4|https://bugs.mojang.com/projects/MC/issues/MC-4]") {
                    replacedCommentBody = it
                    Unit.right()
                }
            )
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedCommentBody shouldBe "Duplicates MC-4"
    }
    "should replace multiple comments" {
        var replacedCommentBody0: String? = null
        var replacedCommentBody1: String? = null
        var replacedCommentBody2: String? = null

        val request = ReplaceCommentModule.Request(
            42L,
            listOf(
                ReplaceCommentModule.Comment(100L, "This is a duplicate of [MC-4|https://bugs.mojang.com/browse/MC-4]") {
                    replacedCommentBody0 = it
                    Unit.right()
                },
                ReplaceCommentModule.Comment(200L, "Check https://bugs.mojang.com/browse/MC-106013 too") {
                    replacedCommentBody1 = it
                    Unit.right()
                },
                ReplaceCommentModule.Comment(300L, "Oops, sorry!") {
                    replacedCommentBody2 = it
                    Unit.right()
                }
            )
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedCommentBody0 shouldBe "This is a duplicate of MC-4"
        replacedCommentBody1 shouldBe "Check MC-106013 too"
        replacedCommentBody2 shouldBe null
    }
})
