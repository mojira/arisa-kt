package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.modules.TransferVersionsModule.Link
import io.github.mojira.arisa.modules.TransferVersionsModule.LinkedIssue
import io.github.mojira.arisa.modules.TransferVersionsModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class TransferVersionsTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there are no issue links" {
        val module = TransferVersionsModule()
        val request = Request(emptyList(), listOf("v1"))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no duplicates link" {
        val module = TransferVersionsModule()
        val link = Link("Relates", true) { LinkedIssue(emptyList()) { Unit.right() }.right() }
        val request = Request(listOf(link), listOf("v1"))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no outgoing duplicates link" {
        val module = TransferVersionsModule()
        val link = Link("Duplicate", false) { LinkedIssue(emptyList()) { Unit.right() }.right() }
        val request = Request(listOf(link), listOf("v1"))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the issue has no affected versions" {
        val module = TransferVersionsModule()
        val link = Link("Duplicate", true) { LinkedIssue(emptyList()) { Unit.right() }.right() }
        val request = Request(listOf(link), emptyList())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the parent already has all versions" {
        val module = TransferVersionsModule()
        val link = Link("Duplicate", true) { LinkedIssue(listOf("v1")) { Unit.right() }.right() }
        val request = Request(listOf(link), listOf("v1"))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return ModuleResponse when there are versions to transfer" {
        val module = TransferVersionsModule()
        val link = Link("Duplicate", true) { LinkedIssue(emptyList()) { Unit.right() }.right() }
        val request = Request(listOf(link), listOf("v1"))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should add all versions to parent" {
        var firstVersionAdded = false
        var secondVersionAdded = false
        val module = TransferVersionsModule()
        val link = Link("Duplicate", true) {
            LinkedIssue(emptyList()) { v ->
                when (v) {
                    "v1" -> firstVersionAdded = true
                    "v2" -> secondVersionAdded = true
                }
                Unit.right()
            }.right()
        }
        val request = Request(listOf(link), listOf("v1", "v2"))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        firstVersionAdded.shouldBeTrue()
        secondVersionAdded.shouldBeTrue()
    }

    "should add versions to all parents" {
        var addedToFirstParent = false
        var addedToSecondParent = false
        val module = TransferVersionsModule()
        val link1 = Link("Duplicate", true) {
            addedToFirstParent = true
            LinkedIssue(emptyList()) {
                Unit.right()
            }.right()
        }
        val link2 = Link("Duplicate", true) {
            addedToSecondParent = true
            LinkedIssue(emptyList()) {
                Unit.right()
            }.right()
        }
        val request = Request(listOf(link1, link2), listOf("v1"))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        addedToFirstParent.shouldBeTrue()
        addedToSecondParent.shouldBeTrue()
    }

    "should return FailedModuleResponse when adding a version fails" {
        val module = TransferVersionsModule()
        val link = Link("Duplicate", true) { LinkedIssue(emptyList()) { RuntimeException().left() }.right() }
        val request = Request(listOf(link), listOf(""))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all errors when adding multiple versions fails" {
        val module = TransferVersionsModule()
        val link = Link("Duplicate", true) { LinkedIssue(emptyList()) { RuntimeException().left() }.right() }
        val request = Request(listOf(link), listOf("v1", "v2"))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should return FailedModuleResponse when getting an issue fails" {
        val module = TransferVersionsModule()
        val link = Link("Duplicate", true) { RuntimeException().left() }
        val request = Request(listOf(link), listOf("v1"))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all errors when getting an issue fails" {
        val module = TransferVersionsModule()
        val link = Link("Duplicate", true) { RuntimeException().left() }
        val request = Request(listOf(link, link), listOf("v1"))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }
})