package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.right
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.modules.ReplaceTextModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

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
                getComment("https://bugs.mojang.com/browse/MC-1", 1L) { Unit.right() }
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
                getComment("MC-1", 100L) { Unit.right() }
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
                getComment(
                    "[A similar issue with this was fixed previously|https://bugs.mojang.com/browse/MC-4]",
                    100L
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
                getComment("[MC-5|https://bugs.mojang.com/browse/MC-4]", 100L) { Unit.right() }
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
                getComment(
                    "[MC-5|https://bugs.mojang.com/projects/MC/issues/MC-4]",
                    100L
                ) { Unit.right() }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for /browse links with query paramerters in comments" {
        val request = Request(
            42L,
            null,
            listOf(
                getComment(
                    "Duplicates https://bugs.mojang.com/browse/MCPE-38374?focusedgetCommentId=555054&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-555054",
                    100L
                ) { Unit.right() }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for /projects links with query paramerters in comments" {
        val request = Request(
            42L,
            null,
            listOf(
                getComment(
                    "Duplicates https://bugs.mojang.com/projects/MC/issues/MC-4?focusedgetCommentId=555054&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-555054",
                    100L
                ) { Unit.right() }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for /browse links which ends in a slash with query paramerters in comments" {
        val request = Request(
            42L,
            null,
            listOf(
                getComment(
                    "Duplicates https://bugs.mojang.com/browse/MCPE-38374/?focusedgetCommentId=555054&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-555054",
                    100L
                ) { Unit.right() }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for /projects links which ends in a slash with query paramerters in comments" {
        val request = Request(
            42L,
            null,
            listOf(
                getComment(
                    "Duplicates https://bugs.mojang.com/projects/MC/issues/MC-4/?focusedgetCommentId=555054&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-555054",
                    100L
                ) { Unit.right() }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for /browse links with query in description" {
        val request = Request(
            42L,
            "Duplicates https://bugs.mojang.com/browse/MCPE-38374?focusedgetCommentId=555054&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-555054",
            emptyList()
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should replace /browse links in comments" {
        var replacedgetCommentBody: String? = null

        val request = Request(
            42L,
            null,
            listOf(
                getComment("Duplicates https://bugs.mojang.com/browse/MC-4", 100L) {
                    replacedgetCommentBody = it
                    Unit.right()
                }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedgetCommentBody shouldBe "Duplicates MC-4"
    }

    "should replace /projects links in comments" {
        var replacedgetCommentBody: String? = null

        val request = Request(
            42L,
            null,
            listOf(
                getComment("Duplicates https://bugs.mojang.com/projects/MC/issues/MC-4", 100L) {
                    replacedgetCommentBody = it
                    Unit.right()
                }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedgetCommentBody shouldBe "Duplicates MC-4"
    }

    "should replace /browse links with two-digit ticket key in comments" {
        var replacedgetCommentBody: String? = null

        val request = Request(
            42L,
            null,
            listOf(
                getComment("Duplicates https://bugs.mojang.com/browse/MC-44", 100L) {
                    replacedgetCommentBody = it
                    Unit.right()
                }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedgetCommentBody shouldBe "Duplicates MC-44"
    }

    "should replace /projects links with two-digit ticket key in comments" {
        var replacedgetCommentBody: String? = null

        val request = Request(
            42L,
            null,
            listOf(
                getComment("Duplicates https://bugs.mojang.com/projects/MC/issues/MC-44", 100L) {
                    replacedgetCommentBody = it
                    Unit.right()
                }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedgetCommentBody shouldBe "Duplicates MC-44"
    }

    "should replace /browse links which ends with a slash in comments" {
        var replacedgetCommentBody: String? = null

        val request = Request(
            42L,
            null,
            listOf(
                getComment("Duplicates https://bugs.mojang.com/browse/MC-4/", 100L) {
                    replacedgetCommentBody = it
                    Unit.right()
                }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedgetCommentBody shouldBe "Duplicates MC-4"
    }

    "should replace /projects links which ends with a slash in comments" {
        var replacedgetCommentBody: String? = null

        val request = Request(
            42L,
            null,
            listOf(
                getComment("Duplicates https://bugs.mojang.com/projects/MC/issues/MC-4/", 100L) {
                    replacedgetCommentBody = it
                    Unit.right()
                }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedgetCommentBody shouldBe "Duplicates MC-4"
    }

    "should replace /browse links with two-digit ticket key which ends with a slash in comments" {
        var replacedgetCommentBody: String? = null

        val request = Request(
            42L,
            null,
            listOf(
                getComment("Duplicates https://bugs.mojang.com/browse/MC-44/", 100L) {
                    replacedgetCommentBody = it
                    Unit.right()
                }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedgetCommentBody shouldBe "Duplicates MC-44"
    }

    "should replace /projects links with two-digit ticket key which ends with a slash in comments" {
        var replacedgetCommentBody: String? = null

        val request = Request(
            42L,
            null,
            listOf(
                getComment("Duplicates https://bugs.mojang.com/projects/MC/issues/MC-44/", 100L) {
                    replacedgetCommentBody = it
                    Unit.right()
                }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedgetCommentBody shouldBe "Duplicates MC-44"
    }

    "should replace titled /browse links in comments" {
        var replacedgetCommentBody: String? = null

        val request = Request(
            42L,
            null,
            listOf(
                getComment("Duplicates [MC-4|https://bugs.mojang.com/browse/MC-4]", 100L) {
                    replacedgetCommentBody = it
                    Unit.right()
                }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedgetCommentBody shouldBe "Duplicates MC-4"
    }

    "should replace titled /projects links in comments" {
        var replacedgetCommentBody: String? = null

        val request = Request(
            42L,
            null,
            listOf(
                getComment("Duplicates [MC-4|https://bugs.mojang.com/projects/MC/issues/MC-4]", 100L) {
                    replacedgetCommentBody = it
                    Unit.right()
                }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedgetCommentBody shouldBe "Duplicates MC-4"
    }

    "should replace multiple comments" {
        var replacedgetCommentBody0: String? = null
        var replacedgetCommentBody1: String? = null
        var replacedgetCommentBody2: String? = null

        val request = Request(
            42L,
            null,
            listOf(
                getComment("This is a duplicate of [MC-4|https://bugs.mojang.com/browse/MC-4]", 100L) {
                    replacedgetCommentBody0 = it
                    Unit.right()
                },
                getComment("Check https://bugs.mojang.com/browse/MC-106013 too", 200L) {
                    replacedgetCommentBody1 = it
                    Unit.right()
                },
                getComment("Oops, sorry!", 300L) {
                    replacedgetCommentBody2 = it
                    Unit.right()
                }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        replacedgetCommentBody0 shouldBe "This is a duplicate of MC-4"
        replacedgetCommentBody1 shouldBe "Check MC-106013 too"
        replacedgetCommentBody2 shouldBe null
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

        var replacedgetCommentBody: String? = null

        val request = Request(
            42L,
            "A mod in https://bugs.mojang.com/browse/MC-4 said that I have to report it in a new ticket.",
            listOf(
                getComment("This is a duplicate of [MC-4|https://bugs.mojang.com/browse/MC-4]", 100L) {
                    replacedgetCommentBody = it
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
        replacedgetCommentBody shouldBe "This is a duplicate of MC-4"
    }
})

private fun getComment(
    body: String,
    updated: Long,
    update: (String) -> Either<Throwable, Unit>
) =
    Comment(body, "", { null }, Instant.now(), Instant.ofEpochMilli(updated), null, null, { Unit.right() }, update)
