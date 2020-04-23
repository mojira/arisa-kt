package io.github.mojira.arisa.modules

import arrow.core.right
import io.github.mojira.arisa.modules.ReplaceTextModule.Comment
import io.github.mojira.arisa.modules.ReplaceTextModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReplaceTextModuleTest : StringSpec({
    val module = ReplaceTextModule()
    "should return OperationNotNeededModuleResponse when there is no description nor comment" {
        val request = Request(
            42L,
            null,
            emptyList()
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
    "should return OperationNotNeededModuleResponse when the comment is updated before last run" {
        val request = Request(
            42L,
            null,
            listOf(
                Comment(1L, "https://bugs.mojang.com/browse/MC-1") { Unit.right() }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
    "should return OperationNotNeededModuleResponse when the comment doesn't need replace" {
        val request = Request(
            42L,
            null,
            listOf(
                Comment(100L, "MC-1") { Unit.right() }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
    "should return OperationNotNeededModuleResponse when the title of the link is not a ticket ID" {
        val request = Request(
            42L,
            null,
            listOf(
                Comment(
                    100L,
                    "[A similar issue with this was fixed previously|https://bugs.mojang.com/browse/MC-4]"
                ) { Unit.right() }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
    "should return OperationNotNeededModuleResponse when the title of the link is not the same ticket as specified in the /browse link" {
        val request = Request(
            42L,
            null,
            listOf(
                Comment(100L, "[MC-5|https://bugs.mojang.com/browse/MC-4]") { Unit.right() }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
    "should return OperationNotNeededModuleResponse when the title of the link is not the same ticket as specified in the /projects link" {
        val request = Request(
            42L,
            null,
            listOf(
                Comment(
                    100L,
                    "[MC-5|https://bugs.mojang.com/projects/MC/issues/MC-4]"
                ) { Unit.right() }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
    "should replace /browse links in comments" {
        var replacedCommentBody: String? = null

        val request = Request(
            42L,
            null,
            listOf(
                Comment(100L, "Duplicates https://bugs.mojang.com/browse/MC-4") {
                    replacedCommentBody = it
                    Unit.right()
                }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedCommentBody shouldBe "Duplicates MC-4"
    }
    "should replace /projects links in comments" {
        var replacedCommentBody: String? = null

        val request = Request(
            42L,
            null,
            listOf(
                Comment(100L, "Duplicates https://bugs.mojang.com/projects/MC/issues/MC-4") {
                    replacedCommentBody = it
                    Unit.right()
                }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedCommentBody shouldBe "Duplicates MC-4"
    }
    "should replace /browse links with query in comments" {
        var replacedCommentBody: String? = null

        val request = Request(
            42L,
            null,
            listOf(
                Comment(100L, "Duplicates https://bugs.mojang.com/browse/MC-4?jql=votes%3E0") {
                    replacedCommentBody = it
                    Unit.right()
                }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedCommentBody shouldBe "Duplicates MC-4"
    }
    "should replace /projects links with query in comments" {
        var replacedCommentBody: String? = null

        val request = Request(
            42L,
            null,
            listOf(
                Comment(
                    100L,
                    "Duplicates https://bugs.mojang.com/projects/MC/issues/MC-4?jql=votes%3E0"
                ) {
                    replacedCommentBody = it
                    Unit.right()
                }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedCommentBody shouldBe "Duplicates MC-4"
    }
    "should replace titled /browse links in comments" {
        var replacedCommentBody: String? = null

        val request = Request(
            42L,
            null,
            listOf(
                Comment(100L, "Duplicates [MC-4|https://bugs.mojang.com/browse/MC-4]") {
                    replacedCommentBody = it
                    Unit.right()
                }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedCommentBody shouldBe "Duplicates MC-4"
    }
    "should replace titled /projects links in comments" {
        var replacedCommentBody: String? = null

        val request = Request(
            42L,
            null,
            listOf(
                Comment(100L, "Duplicates [MC-4|https://bugs.mojang.com/projects/MC/issues/MC-4]") {
                    replacedCommentBody = it
                    Unit.right()
                }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedCommentBody shouldBe "Duplicates MC-4"
    }
    "should replace multiple comments" {
        var replacedCommentBody0: String? = null
        var replacedCommentBody1: String? = null
        var replacedCommentBody2: String? = null

        val request = Request(
            42L,
            null,
            listOf(
                Comment(100L, "This is a duplicate of [MC-4|https://bugs.mojang.com/browse/MC-4]") {
                    replacedCommentBody0 = it
                    Unit.right()
                },
                Comment(200L, "Check https://bugs.mojang.com/browse/MC-106013 too") {
                    replacedCommentBody1 = it
                    Unit.right()
                },
                Comment(300L, "Oops, sorry!") {
                    replacedCommentBody2 = it
                    Unit.right()
                }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedCommentBody0 shouldBe "This is a duplicate of MC-4"
        replacedCommentBody1 shouldBe "Check MC-106013 too"
        replacedCommentBody2 shouldBe null
    }
    "should replace /browse links in description" {
        var replacedDescription: String? = null

        val request = Request(
            42L,
            "A mod in https://bugs.mojang.com/browse/MC-4 said that I have to report it in a new ticket.",
            emptyList()
        ) {
            replacedDescription = it
            Unit.right()
        }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedDescription shouldBe "A mod in MC-4 said that I have to report it in a new ticket."
    }
    "should replace both description and comments" {
        var replacedDescription: String? = null

        var replacedCommentBody: String? = null

        val request = Request(
            42L,
            "A mod in https://bugs.mojang.com/browse/MC-4 said that I have to report it in a new ticket.",
            listOf(
                Comment(100L, "This is a duplicate of [MC-4|https://bugs.mojang.com/browse/MC-4]") {
                    replacedCommentBody = it
                    Unit.right()
                }
            )
        ) {
            replacedDescription = it
            Unit.right()
        }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedDescription shouldBe "A mod in MC-4 said that I have to report it in a new ticket."
        replacedCommentBody shouldBe "This is a duplicate of MC-4"
    }
})
