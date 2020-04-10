package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.modules.ReopenAwaitingModule.Comment
import io.github.mojira.arisa.modules.ReopenAwaitingModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant

class ReopenAwaitingModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no resolution" {
        val module = ReopenAwaitingModule()
        val now = Instant.now()
        val updated = Instant.now().plusSeconds(3)
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli())
        val request = Request(null, now, updated, listOf(comment)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is not in awaiting response" {
        val module = ReopenAwaitingModule()
        val now = Instant.now()
        val updated = Instant.now().plusSeconds(3)
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli())
        val request = Request("Test", now, updated, listOf(comment)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is less than 2 seconds old" {
        val module = ReopenAwaitingModule()
        val now = Instant.now()
        val updated = Instant.now().plusSeconds(1)
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli())
        val request = Request("Awaiting Response", now, updated, listOf(comment)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there are no comments" {
        val module = ReopenAwaitingModule()
        val now = Instant.now()
        val updated = Instant.now().plusSeconds(3)
        val request = Request("Awaiting Response", now, updated, emptyList()) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when just the comment was updated" {
        val module = ReopenAwaitingModule()
        val now = Instant.now()
        val comment = Comment(now.plusSeconds(3).toEpochMilli(), now.toEpochMilli())
        val updated = Instant.now().plusSeconds(3)
        val request = Request("Awaiting Response", now, updated, listOf(comment)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should grab the last comment" {
        val module = ReopenAwaitingModule()
        val now = Instant.now()
        val updated = Instant.now().plusSeconds(3)
        val commentFail = Comment(now.plusSeconds(3).toEpochMilli(), now.toEpochMilli())
        val commentSuccess = Comment(now.toEpochMilli(), now.toEpochMilli())
        val request = Request("Awaiting Response", now, updated, listOf(commentSuccess, commentFail)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return FailedModuleResponse with all exceptions when reopening fails" {
        val module = ReopenAwaitingModule()
        val now = Instant.now()
        val updated = Instant.now().plusSeconds(3)
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli())
        val request = Request("Awaiting Response", now, updated, listOf(comment)) { RuntimeException().left() }

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return ModuleResponse when ticket is reopened" {
        val module = ReopenAwaitingModule()
        val now = Instant.now()
        val updated = Instant.now().plusSeconds(3)
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli())
        val request = Request("Awaiting Response", now, updated, listOf(comment)) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }
})
