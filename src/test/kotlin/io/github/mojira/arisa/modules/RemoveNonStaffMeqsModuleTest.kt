package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.User
import io.github.mojira.arisa.modules.RemoveNonStaffMeqsModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant

class RemoveNonStaffMeqsModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no comments" {
        val module = RemoveNonStaffMeqsModule("")
        val request = Request(emptyList())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no comments with an MEQS tag" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = Comment(
            "I like QC.",
            User("user", ""),
            { emptyList() },
            Instant.now(),
            Instant.now(),
            null,
            null,
            { Unit.right() },
            { Unit.right() }
        )
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for a staff restricted comment" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = Comment(
            "MEQS_WAI I like QC.",
            User("user", ""),
            { emptyList() },
            Instant.now(),
            Instant.now(),
            "group",
            "staff",
            { Unit.right() },
            { Unit.right() }
        )
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if MEQS is not part of a tag" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = Comment(
            "My server has 1 MEQS of RAM and it's crashing. Also I don't know how to spell MEGS",
            User("user", ""),
            { emptyList() },
            Instant.now(),
            Instant.now(),
            null,
            null,
            { Unit.right() },
            { Unit.right() }
        )
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return FailedModuleResponse when updating fails" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = Comment(
            "MEQS_WAI I like QC.",
            User("user", ""),
            { emptyList() },
            Instant.now(),
            Instant.now(),
            null,
            null,
            { RuntimeException().left() },
            { RuntimeException().left() }
        )
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all exceptions when updating fails" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = Comment(
            "MEQS_WAI I like QC.",
            User("user", ""),
            { emptyList() },
            Instant.now(),
            Instant.now(),
            null,
            null,
            { RuntimeException().left() },
            { RuntimeException().left() }
        )
        val request = Request(listOf(comment, comment))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should update comment when there is an unrestricted MEQS comment" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = Comment(
            "MEQS_WAI I like QC.",
            User("user", ""),
            { emptyList() },
            Instant.now(),
            Instant.now(),
            null,
            null,
            { Unit.right() },
            { Unit.right() }
        )
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should update comment when there is a MEQS comment restricted to a group other than staff" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = Comment(
            "MEQS_WAI I like QC.",
            User("user", ""),
            { emptyList() },
            Instant.now(),
            Instant.now(),
            "group",
            "users",
            { Unit.right() },
            { Unit.right() }
        )
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should update comment when there is a MEQS comment restricted to something that is not a group" {
        val module = RemoveNonStaffMeqsModule("")
        val comment = Comment(
            "MEQS_WAI I like QC.",
            User("user", ""),
            { emptyList() },
            Instant.now(),
            Instant.now(),
            "user",
            "staff",
            { Unit.right() },
            { Unit.right() }
        )
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should only remove MEQS of the comment" {
        val module = RemoveNonStaffMeqsModule("Test.")
        val comment = Comment(
            "MEQS_WAI\nI like QC.",
            User("user", ""),
            { emptyList() },
            Instant.now(),
            Instant.now(),
            null,
            null,
            { it.shouldBe("MEQS_ARISA_REMOVED_WAI Removal Reason: Test.\nI like QC."); Unit.right() },
            { Unit.right() }
        )
        val request = Request(listOf(comment))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }
})
