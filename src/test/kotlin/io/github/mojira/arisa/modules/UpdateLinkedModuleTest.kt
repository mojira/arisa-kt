package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.modules.UpdateLinkedModule.ChangeLogItem
import io.github.mojira.arisa.modules.UpdateLinkedModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.temporal.ChronoUnit

class UpdateLinkedModuleTest : StringSpec({
    val NOW = Instant.now()
    val A_SECOND_AGO = NOW.minusSeconds(1)
    val DUPLICATE_LINK = ChangeLogItem("Link", NOW, null, "This issue is duplicated by MC-4")

    "should return OperationNotNeededModuleResponse when linked is empty and there are no duplicates" {
        val module = UpdateLinkedModule(0)
        val request = Request(A_SECOND_AGO, emptyList(), null) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when linked is 0 and there are no duplicates" {
        val module = UpdateLinkedModule(0)
        val request = Request(A_SECOND_AGO, emptyList(), 0.0) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when linked and number of duplicates is equal" {
        val module = UpdateLinkedModule(0)
        val request = Request(A_SECOND_AGO, listOf(DUPLICATE_LINK), 1.0) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is only a recently added link" {
        val module = UpdateLinkedModule(1)
        val request = Request(A_SECOND_AGO, listOf(DUPLICATE_LINK), 0.0) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is only a recently added link after the last Linked change" {
        val module = UpdateLinkedModule(1)
        val linkedChange = ChangeLogItem("Linked", NOW.minusSeconds(2), null, "1.0")
        val oldAddedLink = ChangeLogItem("Link", NOW.minus(2, ChronoUnit.HOURS), null, "This issue is duplicated by MC-4")
        val request = Request(NOW.minus(3, ChronoUnit.HOURS), listOf(oldAddedLink, linkedChange, DUPLICATE_LINK), 0.0) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should set linked when there are duplicates and linked is empty" {
        val module = UpdateLinkedModule(0)
        val request = Request(A_SECOND_AGO, listOf(DUPLICATE_LINK), null) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should set linked when there are duplicates and linked is too low" {
        var linked = 0.0
        val module = UpdateLinkedModule(0)
        val request = Request(A_SECOND_AGO, listOf(DUPLICATE_LINK), 0.0) { linked = it; Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        linked shouldBe 1.0
    }

    "should set linked when there are duplicates and linked is too high" {
        var linked = 1.0
        val module = UpdateLinkedModule(0)
        val removedLink = ChangeLogItem("Link", NOW.plusSeconds(1), "This issue is duplicated by MC-4", null)
        val request = Request(A_SECOND_AGO, listOf(DUPLICATE_LINK, removedLink), 1.0) { linked = it; Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        linked shouldBe 0.0
    }

    "should only count duplicates" {
        var linked = 0.0
        val module = UpdateLinkedModule(0)
        val relatesLink = ChangeLogItem("Link", NOW, null, "This issue relates to MC-4")
        val request = Request(A_SECOND_AGO, listOf(DUPLICATE_LINK, relatesLink, DUPLICATE_LINK), null) { linked = it; Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        linked shouldBe 2.0
    }

    "should update if there is an old and a recent link" {
        val module = UpdateLinkedModule(1)
        val oldAddedLink = ChangeLogItem("Link", NOW.minus(2, ChronoUnit.HOURS), null, "This issue is duplicated by MC-4")
        val request = Request(A_SECOND_AGO.minus(2, ChronoUnit.HOURS), listOf(oldAddedLink, DUPLICATE_LINK), null) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should update if a link was removed" {
        var linked = 1.0
        val module = UpdateLinkedModule(1)
        val addedLink = ChangeLogItem("Link", NOW.minus(4, ChronoUnit.HOURS), null, "This issue is duplicated by MC-4")
        val linkedChange = ChangeLogItem("Linked", A_SECOND_AGO.minus(3, ChronoUnit.HOURS), null, "1.0")
        val removedLink = ChangeLogItem("Link", NOW.minus(2, ChronoUnit.HOURS), "This issue is duplicated by MC-4", null)
        val request = Request(A_SECOND_AGO.minus(4, ChronoUnit.HOURS), listOf(addedLink, linkedChange, removedLink), 1.0) { linked = it; Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        linked shouldBe 0.0
    }

    "should return FailedModuleResponse when setting linked fails" {
        val module = UpdateLinkedModule(0)
        val request = Request(A_SECOND_AGO, listOf(DUPLICATE_LINK), null) { RuntimeException().left() }

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
