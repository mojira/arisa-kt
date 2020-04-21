package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class UpdateLinkedModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when linked is empty and there are no duplicates" {
        val module = UpdateLinkedModule()
        val request = UpdateLinkedModule.Request("Piston", emptyList(), null) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when linked is 0 and there are no duplicates" {
        val module = UpdateLinkedModule()
        val request = UpdateLinkedModule.Request("Piston", emptyList(), 0.0) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when linked and number of duplicates is equal" {
        val module = UpdateLinkedModule()
        val request = UpdateLinkedModule.Request(
            "Piston",
            listOf(UpdateLinkedModule.IssueLink("Arisa", "Duplicate")),
            1.0
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should set linked when there are duplicates and linked is empty" {
        val module = UpdateLinkedModule()
        val request = UpdateLinkedModule.Request(
            "Piston",
            listOf(UpdateLinkedModule.IssueLink("Arisa", "Duplicate")),
            null
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should set linked when there are duplicates and linked is too low" {
        val module = UpdateLinkedModule()
        val request = UpdateLinkedModule.Request(
            "Piston",
            listOf(UpdateLinkedModule.IssueLink("Arisa", "Duplicate")),
            0.0
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should set linked when there are duplicates and linked is too high" {
        val module = UpdateLinkedModule()
        val request = UpdateLinkedModule.Request(
            "Piston",
            listOf(UpdateLinkedModule.IssueLink("Arisa", "Duplicate")),
            2.0
        ) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should only count duplicates" {
        var linked = 0.0
        val module = UpdateLinkedModule()
        val request = UpdateLinkedModule.Request(
            "Piston",
            listOf(
                UpdateLinkedModule.IssueLink("Arisa", "Duplicate"),
                UpdateLinkedModule.IssueLink("Arisa", "Relates"),
                UpdateLinkedModule.IssueLink("Arisa", "Duplicate")
            ),
            null
        ) { linked = it; Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        linked shouldBe 2.0
    }

    "should only count tickets reported by different users" {
        var linked = 0.0
        val module = UpdateLinkedModule()
        val request = UpdateLinkedModule.Request(
            "Piston",
            listOf(
                UpdateLinkedModule.IssueLink("Arisa", "Duplicate"),
                UpdateLinkedModule.IssueLink("Piston", "Duplicate"),
                UpdateLinkedModule.IssueLink("Arisa", "Duplicate")
            ),
            null
        ) { linked = it; Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        linked shouldBe 2.0
    }

    "should return FailedModuleResponse when setting linked fails" {
        val module = UpdateLinkedModule()
        val request = UpdateLinkedModule.Request(
            "Piston",
            listOf(UpdateLinkedModule.IssueLink("Arisa", "Duplicate")),
            null
        ) { RuntimeException().left() }

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
