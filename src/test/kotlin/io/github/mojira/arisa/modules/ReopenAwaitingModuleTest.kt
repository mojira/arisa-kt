package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.modules.ReopenAwaitingModule.ChangeLogItem
import io.github.mojira.arisa.modules.ReopenAwaitingModule.Comment
import io.github.mojira.arisa.modules.ReopenAwaitingModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant

class ReopenAwaitingModuleTest : StringSpec({
    val NOW = Instant.now()
    val COMMENT = Comment(NOW.toEpochMilli(), NOW.toEpochMilli())
    val AWAITING_RESOLVE = ChangeLogItem(NOW.minusSeconds(10).toEpochMilli(), "Awaiting Response")
    "should return OperationNotNeededModuleResponse when there is no resolution" {
        val module = ReopenAwaitingModule()
        val updated = NOW.plusSeconds(3)
        val request = Request(null, NOW, updated, listOf(COMMENT), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is not in awaiting response" {
        val module = ReopenAwaitingModule()
        val updated = NOW.plusSeconds(3)
        val request = Request("Test", NOW, updated, listOf(COMMENT), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is less than 2 seconds old" {
        val module = ReopenAwaitingModule()
        val updated = NOW.plusSeconds(1)
        val request = Request("Awaiting Response", NOW, updated, listOf(COMMENT), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there are no comments" {
        val module = ReopenAwaitingModule()
        val updated = NOW.plusSeconds(3)
        val request = Request("Awaiting Response", NOW, updated, emptyList(), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is only a comment from before the resolve" {
        val module = ReopenAwaitingModule()
        val updated = NOW.plusSeconds(3)
        val comment = Comment(NOW.minusSeconds(20).toEpochMilli(), NOW.minusSeconds(20).toEpochMilli())
        val request = Request("Awaiting Response", NOW, updated, listOf(comment), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there were multiple resolves, but no comment after the last resolve." {
        val module = ReopenAwaitingModule()
        val updated = NOW.plusSeconds(3)
        val oldResolve = ChangeLogItem(NOW.minusSeconds(30).toEpochMilli(), "Awaiting Response")
        val comment = Comment(NOW.minusSeconds(20).toEpochMilli(), NOW.minusSeconds(20).toEpochMilli())
        val request = Request("Awaiting Response", NOW, updated, listOf(comment), listOf(oldResolve, AWAITING_RESOLVE)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when just the comment was updated" {
        val module = ReopenAwaitingModule()
        val comment = Comment(NOW.plusSeconds(3).toEpochMilli(), NOW.toEpochMilli())
        val updated = NOW.plusSeconds(3)
        val request = Request("Awaiting Response", NOW, updated, listOf(comment), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should grab the last comment" {
        val module = ReopenAwaitingModule()
        val updated = NOW.plusSeconds(3)
        val commentFail = Comment(NOW.plusSeconds(3).toEpochMilli(), NOW.toEpochMilli())
        val commentSuccess = Comment(NOW.toEpochMilli(), NOW.toEpochMilli())
        val request = Request("Awaiting Response", NOW, updated, listOf(commentSuccess, commentFail), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should ignore changes that are not a resolve" {
        val module = ReopenAwaitingModule()
        val updated = NOW.plusSeconds(3)
        val change = ChangeLogItem(NOW.plusSeconds(3).toEpochMilli(), "Confirmed")
        val request = Request("Awaiting Response", NOW, updated, listOf(COMMENT), listOf(AWAITING_RESOLVE, change)) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse with all exceptions when reopening fails" {
        val module = ReopenAwaitingModule()
        val updated = NOW.plusSeconds(3)
        val request = Request("Awaiting Response", NOW, updated, listOf(COMMENT), listOf(AWAITING_RESOLVE)) { RuntimeException().left() }

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return ModuleResponse when ticket is reopened" {
        val module = ReopenAwaitingModule()
        val updated = NOW.plusSeconds(3)
        val request = Request("Awaiting Response", NOW, updated, listOf(COMMENT), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }
})
