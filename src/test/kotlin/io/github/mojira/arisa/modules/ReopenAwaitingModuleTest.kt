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
    val MODULE = ReopenAwaitingModule(
        listOf("staff", "global-moderators"),
        listOf("helper", "staff", "global-moderators")
    )

    "should return OperationNotNeededModuleResponse when there is no resolution" {
        val now = Instant.now()
        val updated = Instant.now().plusSeconds(3)
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli(), null, null, null)
        val request = Request(null, now, updated, listOf(comment)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is not in awaiting response" {
        val now = Instant.now()
        val updated = Instant.now().plusSeconds(3)
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli(), null, null, null)
        val request = Request("Test", now, updated, listOf(comment)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is less than 2 seconds old" {
        val now = Instant.now()
        val updated = Instant.now().plusSeconds(1)
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli(), null, null, null)
        val request = Request("Awaiting Response", now, updated, listOf(comment)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there are no comments" {
        val now = Instant.now()
        val updated = Instant.now().plusSeconds(3)
        val request = Request("Awaiting Response", now, updated, emptyList()) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when just the comment was updated" {
        val now = Instant.now()
        val comment = Comment(now.plusSeconds(3).toEpochMilli(), now.toEpochMilli(), null, null, null)
        val updated = Instant.now().plusSeconds(3)
        val request = Request("Awaiting Response", now, updated, listOf(comment)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment is restricted" {
        val now = Instant.now()
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli(), "group", "helper", null)
        val updated = Instant.now().plusSeconds(3)
        val request = Request("Awaiting Response", now, updated, listOf(comment)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment author is staff" {
        val now = Instant.now()
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli(), null, null, listOf("staff"))
        val updated = Instant.now().plusSeconds(3)
        val request = Request("Awaiting Response", now, updated, listOf(comment)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should grab the last comment" {
        val now = Instant.now()
        val updated = Instant.now().plusSeconds(3)
        val commentFail = Comment(now.plusSeconds(3).toEpochMilli(), now.toEpochMilli(), null, null, null)
        val commentSuccess = Comment(now.toEpochMilli(), now.toEpochMilli(), null, null, null)
        val request = Request("Awaiting Response", now, updated, listOf(commentSuccess, commentFail)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return FailedModuleResponse with all exceptions when reopening fails" {
        val now = Instant.now()
        val updated = Instant.now().plusSeconds(3)
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli(), null, null, null)
        val request = Request("Awaiting Response", now, updated, listOf(comment)) { RuntimeException().left() }

        val result = MODULE(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should reopen when someone answered" {
        val now = Instant.now()
        val updated = Instant.now().plusSeconds(3)
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli(), null, null, null)
        val request = Request("Awaiting Response", now, updated, listOf(comment)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment is restricted, but not to a group" {
        val now = Instant.now()
        val updated = Instant.now().plusSeconds(3)
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli(), "not-a-group", "helper", null)
        val request = Request("Awaiting Response", now, updated, listOf(comment)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment is restricted, but not to a blacklisted group" {
        val now = Instant.now()
        val updated = Instant.now().plusSeconds(3)
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli(), "group", "users", null)
        val request = Request("Awaiting Response", now, updated, listOf(comment)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment author has no groups" {
        val now = Instant.now()
        val updated = Instant.now().plusSeconds(3)
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli(), null, null, emptyList())
        val request = Request("Awaiting Response", now, updated, listOf(comment)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment author has no blacklisted groups" {
        val now = Instant.now()
        val updated = Instant.now().plusSeconds(3)
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli(), null, null, listOf("Users"))
        val request = Request("Awaiting Response", now, updated, listOf(comment)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }
})
