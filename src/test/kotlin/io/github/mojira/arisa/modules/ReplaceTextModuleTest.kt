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
    val NOW = Instant.now()
    val A_SECOND_AGO = NOW.minusSeconds(1)
    val TWO_SECONDs_AGO = NOW.minusSeconds(1)
    
    val module = ReplaceTextModule()
    "should return OperationNotNeededModuleResponse when there is no description nor comment" {
        val request = Request(
            A_SECOND_AGO,
            null,
            emptyList()
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the comment is updated before last run" {
        val request = Request(
            A_SECOND_AGO,
            null,
            listOf(
                getComment("https://bugs.mojang.com/browse/MC-1", TWO_SECONDs_AGO) { Unit.right() }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the comment doesn't need replace" {
        val request = Request(
            A_SECOND_AGO,
            null,
            listOf(
                getComment("MC-1", NOW) { Unit.right() }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the title of the link is not a ticket ID" {
        val request = Request(
            A_SECOND_AGO,
            null,
            listOf(
                getComment(
                    "[A similar issue with this was fixed previously|https://bugs.mojang.com/browse/MC-4]",
                    NOW
                ) { Unit.right() }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the title of the link is not the same ticket as specified in the /browse link" {
        val request = Request(
            A_SECOND_AGO,
            null,
            listOf(
                getComment("[MC-5|https://bugs.mojang.com/browse/MC-4]", NOW) { Unit.right() }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the title of the link is not the same ticket as specified in the /projects link" {
        val request = Request(
            A_SECOND_AGO,
            null,
            listOf(
                getComment(
                    "[MC-5|https://bugs.mojang.com/projects/MC/issues/MC-4]",
                    NOW
                ) { Unit.right() }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for /browse links with query paramerters in comments" {
        val request = Request(
            A_SECOND_AGO,
            null,
            listOf(
                getComment(
                    "Duplicates https://bugs.mojang.com/browse/MCPE-38374?focusedgetCommentId=555054&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-555054",
                    NOW
                ) { Unit.right() }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for /projects links with query paramerters in comments" {
        val request = Request(
            A_SECOND_AGO,
            null,
            listOf(
                getComment(
                    "Duplicates https://bugs.mojang.com/projects/MC/issues/MC-4?focusedgetCommentId=555054&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-555054",
                    NOW
                ) { Unit.right() }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for /browse links which ends in a slash with query paramerters in comments" {
        val request = Request(
            A_SECOND_AGO,
            null,
            listOf(
                getComment(
                    "Duplicates https://bugs.mojang.com/browse/MCPE-38374/?focusedgetCommentId=555054&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-555054",
                    NOW
                ) { Unit.right() }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for /projects links which ends in a slash with query paramerters in comments" {
        val request = Request(
            A_SECOND_AGO,
            null,
            listOf(
                getComment(
                    "Duplicates https://bugs.mojang.com/projects/MC/issues/MC-4/?focusedgetCommentId=555054&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-555054",
                    NOW
                ) { Unit.right() }
            )
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for /browse links with query in description" {
        val request = Request(
            A_SECOND_AGO,
            "Duplicates https://bugs.mojang.com/browse/MCPE-38374?focusedgetCommentId=555054&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-555054",
            emptyList()
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should replace /browse links in comments" {
        var replacedgetCommentBody: String? = null

        val request = Request(
            A_SECOND_AGO,
            null,
            listOf(
                getComment("Duplicates https://bugs.mojang.com/browse/MC-4", NOW) {
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
            A_SECOND_AGO,
            null,
            listOf(
                getComment("Duplicates https://bugs.mojang.com/projects/MC/issues/MC-4", NOW) {
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
            A_SECOND_AGO,
            null,
            listOf(
                getComment("Duplicates https://bugs.mojang.com/browse/MC-44", NOW) {
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
            A_SECOND_AGO,
            null,
            listOf(
                getComment("Duplicates https://bugs.mojang.com/projects/MC/issues/MC-44", NOW) {
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
            A_SECOND_AGO,
            null,
            listOf(
                getComment("Duplicates https://bugs.mojang.com/browse/MC-4/", NOW) {
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
            A_SECOND_AGO,
            null,
            listOf(
                getComment("Duplicates https://bugs.mojang.com/projects/MC/issues/MC-4/", NOW) {
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
            A_SECOND_AGO,
            null,
            listOf(
                getComment("Duplicates https://bugs.mojang.com/browse/MC-44/", NOW) {
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
            A_SECOND_AGO,
            null,
            listOf(
                getComment("Duplicates https://bugs.mojang.com/projects/MC/issues/MC-44/", NOW) {
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
            A_SECOND_AGO,
            null,
            listOf(
                getComment("Duplicates [MC-4|https://bugs.mojang.com/browse/MC-4]", NOW) {
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
            A_SECOND_AGO,
            null,
            listOf(
                getComment("Duplicates [MC-4|https://bugs.mojang.com/projects/MC/issues/MC-4]", NOW) {
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
            A_SECOND_AGO,
            null,
            listOf(
                getComment("This is a duplicate of [MC-4|https://bugs.mojang.com/browse/MC-4]", NOW) {
                    replacedgetCommentBody0 = it
                    Unit.right()
                },
                getComment("Check https://bugs.mojang.com/browse/MC-106013 too", NOW.plusSeconds(1)) {
                    replacedgetCommentBody1 = it
                    Unit.right()
                },
                getComment("Oops, sorry!", NOW.plusSeconds(2)) {
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
            A_SECOND_AGO,
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
            A_SECOND_AGO,
            "A mod in https://bugs.mojang.com/browse/MC-4 said that I have to report it in a new ticket.",
            listOf(
                getComment("This is a duplicate of [MC-4|https://bugs.mojang.com/browse/MC-4]", NOW) {
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
    updated: Instant,
    update: (String) -> Either<Throwable, Unit>
) =
    Comment(body, "", { null }, Instant.now(), updated, null, null, { Unit.right() }, update)
