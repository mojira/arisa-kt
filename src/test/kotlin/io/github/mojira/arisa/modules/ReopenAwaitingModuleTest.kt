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

    val MODULE = ReopenAwaitingModule(
        listOf("staff", "global-moderators"),
        listOf("helper", "staff", "global-moderators")
    )
    val NOW = Instant.now()
    val COMMENT = Comment(NOW.toEpochMilli(), NOW.toEpochMilli(), null, null, null)
    val AWAITING_RESOLVE = ChangeLogItem(NOW.minusSeconds(10).toEpochMilli(), "Awaiting Response")
    "should return OperationNotNeededModuleResponse when there is no resolution" {
        val updated = NOW.plusSeconds(3)
        val request = Request(null, NOW, updated, listOf(COMMENT), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is not in awaiting response" {
        val updated = NOW.plusSeconds(3)
        val request = Request("Test", NOW, updated, listOf(COMMENT), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is less than 2 seconds old" {
        val updated = NOW.plusSeconds(1)
        val request = Request("Awaiting Response", NOW, updated, listOf(COMMENT), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there are no comments" {
        val updated = NOW.plusSeconds(3)
        val request = Request("Awaiting Response", NOW, updated, emptyList(), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is only a comment from before the resolve" {
        val updated = NOW.plusSeconds(3)
        val comment = Comment(
            NOW.minusSeconds(20).toEpochMilli(),
            NOW.minusSeconds(20).toEpochMilli(),
            null,
            null,
            null
        )
        val request = Request("Awaiting Response", NOW, updated, listOf(comment), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there were multiple resolves, but no comment after the last resolve." {
        val updated = NOW.plusSeconds(3)
        val oldResolve = ChangeLogItem(NOW.minusSeconds(30).toEpochMilli(), "Awaiting Response")
        val comment = Comment(
            NOW.minusSeconds(20).toEpochMilli(),
            NOW.minusSeconds(20).toEpochMilli(),
            null,
            null,
            null
        )
        val request = Request("Awaiting Response", NOW, updated, listOf(comment), listOf(oldResolve, AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when just the comment was updated" {
        val comment = Comment(
            NOW.plusSeconds(3).toEpochMilli(),
            NOW.toEpochMilli(),
            null,
            null,
            null
        )
        val updated = NOW.plusSeconds(3)
        val request = Request("Awaiting Response", NOW, updated, listOf(comment), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment is restricted" {
        val now = NOW
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli(), "group", "helper", null)
        val updated = NOW.plusSeconds(3)
        val request = Request("Awaiting Response", now, updated, listOf(comment), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment author is staff" {
        val now = NOW
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli(), null, null, listOf("staff"))
        val updated = NOW.plusSeconds(3)
        val request = Request("Awaiting Response", now, updated, listOf(comment), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return ModuleResponse when ticket is reopened" {
        val updated = NOW.plusSeconds(3)
        val request = Request("Awaiting Response", NOW, updated, listOf(COMMENT), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should grab the last comment" {
        val updated = NOW.plusSeconds(3)
        val commentFail = Comment(
            NOW.plusSeconds(3).toEpochMilli(),
            NOW.toEpochMilli(),
            null,
            null,
            null
        )
        val commentSuccess = Comment(
            NOW.toEpochMilli(),
            NOW.toEpochMilli(),
            null,
            null,
            null
        )
        val request = Request("Awaiting Response", NOW, updated, listOf(commentSuccess, commentFail), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should ignore changes that are not a resolve" {
        val updated = NOW.plusSeconds(3)
        val change = ChangeLogItem(NOW.plusSeconds(3).toEpochMilli(), "Confirmed")
        val request = Request("Awaiting Response", NOW, updated, listOf(COMMENT), listOf(AWAITING_RESOLVE, change)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when someone answered" {
        val now = NOW
        val updated = NOW.plusSeconds(3)
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli(), null, null, null)
        val request = Request("Awaiting Response", now, updated, listOf(comment), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment is restricted, but not to a group" {
        val now = NOW
        val updated = NOW.plusSeconds(3)
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli(), "not-a-group", "helper", null)
        val request = Request("Awaiting Response", now, updated, listOf(comment), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment is restricted, but not to a blacklisted group" {
        val now = NOW
        val updated = NOW.plusSeconds(3)
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli(), "group", "users", null)
        val request = Request("Awaiting Response", now, updated, listOf(comment), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment author has no groups" {
        val now = NOW
        val updated = NOW.plusSeconds(3)
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli(), null, null, emptyList())
        val request = Request("Awaiting Response", now, updated, listOf(comment), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment author has no blacklisted groups" {
        val now = NOW
        val updated = NOW.plusSeconds(3)
        val comment = Comment(now.toEpochMilli(), now.toEpochMilli(), null, null, listOf("Users"))
        val request = Request("Awaiting Response", now, updated, listOf(comment), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse with all exceptions when reopening fails" {
        val updated = NOW.plusSeconds(3)
        val request = Request("Awaiting Response", NOW, updated, listOf(COMMENT), listOf(AWAITING_RESOLVE)) { RuntimeException().left() }

        val result = MODULE(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
