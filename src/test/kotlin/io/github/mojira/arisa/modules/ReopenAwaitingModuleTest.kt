package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.modules.ReopenAwaitingModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant

private val NOW = Instant.now()

class ReopenAwaitingModuleTest : StringSpec({
    val MODULE = ReopenAwaitingModule(
        listOf("staff", "global-moderators"),
        listOf("helper", "staff", "global-moderators")
    )
    val AWAITING_RESOLVE = ChangeLogItem(NOW.minusSeconds(10), "", "", "Awaiting Response") { emptyList() }

    "should return OperationNotNeededModuleResponse when there is no resolution" {
        val updated = NOW.plusSeconds(3)
        val request = Request(null, NOW, updated, listOf(getComment()), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is not in awaiting response" {
        val updated = NOW.plusSeconds(3)
        val request = Request("Test", NOW, updated, listOf(getComment()), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is less than 2 seconds old" {
        val updated = NOW.plusSeconds(1)
        val request =
            Request("Awaiting Response", NOW, updated, listOf(getComment()), listOf(AWAITING_RESOLVE)) { Unit.right() }

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
        val comment = getComment(
            NOW.minusSeconds(20),
            NOW.minusSeconds(20)
        )
        val request =
            Request("Awaiting Response", NOW, updated, listOf(comment), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there were multiple resolves, but no comment after the last resolve." {
        val updated = NOW.plusSeconds(3)
        val oldResolve = ChangeLogItem(NOW.minusSeconds(30), "", "", "Awaiting Response") { emptyList() }
        val comment = getComment(
            NOW.minusSeconds(20),
            NOW.minusSeconds(20)
        )
        val request = Request(
            "Awaiting Response",
            NOW,
            updated,
            listOf(comment),
            listOf(oldResolve, AWAITING_RESOLVE)
        ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when just the comment was updated" {
        val comment = getComment(NOW.plusSeconds(3))
        val updated = NOW.plusSeconds(3)
        val request =
            Request("Awaiting Response", NOW, updated, listOf(comment), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment is restricted" {
        val comment = getComment(visibilityType = "group", visibilityValue = "helper")
        val updated = NOW.plusSeconds(3)
        val request =
            Request("Awaiting Response", NOW, updated, listOf(comment), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment author is staff" {
        val comment = getComment(authorGroups = listOf("staff"))
        val updated = NOW.plusSeconds(3)
        val request =
            Request("Awaiting Response", NOW, updated, listOf(comment), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return ModuleResponse when ticket is reopened" {
        val updated = NOW.plusSeconds(3)
        val request =
            Request("Awaiting Response", NOW, updated, listOf(getComment()), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should grab the last comment" {
        val updated = NOW.plusSeconds(3)
        val commentFail = getComment(NOW.plusSeconds(3))
        val commentSuccess = getComment()
        val request = Request(
            "Awaiting Response",
            NOW,
            updated,
            listOf(commentSuccess, commentFail),
            listOf(AWAITING_RESOLVE)
        ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should ignore changes that are not a resolve" {
        val updated = NOW.plusSeconds(3)
        val change = ChangeLogItem(NOW.plusSeconds(3), "", "", "Confirmed") { emptyList() }
        val request = Request(
            "Awaiting Response",
            NOW,
            updated,
            listOf(getComment()),
            listOf(AWAITING_RESOLVE, change)
        ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when someone answered" {
        val updated = NOW.plusSeconds(3)
        val request =
            Request("Awaiting Response", NOW, updated, listOf(getComment()), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment is restricted, but not to a group" {
        val updated = NOW.plusSeconds(3)
        val comment = getComment(visibilityType = "not-a-group", visibilityValue = "helper")
        val request =
            Request("Awaiting Response", NOW, updated, listOf(comment), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment is restricted, but not to a blacklisted group" {
        val updated = NOW.plusSeconds(3)
        val comment = getComment(visibilityType = "group", visibilityValue = "users")
        val request =
            Request("Awaiting Response", NOW, updated, listOf(comment), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment author has no groups" {
        val updated = NOW.plusSeconds(3)
        val comment = getComment(authorGroups = emptyList())
        val request =
            Request("Awaiting Response", NOW, updated, listOf(comment), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment author has no blacklisted groups" {
        val updated = NOW.plusSeconds(3)
        val comment = getComment(authorGroups = listOf("Users"))
        val request =
            Request("Awaiting Response", NOW, updated, listOf(comment), listOf(AWAITING_RESOLVE)) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse with all exceptions when reopening fails" {
        val updated = NOW.plusSeconds(3)
        val request = Request(
            "Awaiting Response",
            NOW,
            updated,
            listOf(getComment()),
            listOf(AWAITING_RESOLVE)
        ) { RuntimeException().left() }

        val result = MODULE(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})

private fun getComment(
    updated: Instant = NOW,
    created: Instant = NOW,
    visibilityType: String? = null,
    visibilityValue: String? = null,
    authorGroups: List<String>? = null
) = Comment(
    "",
    "",
    { authorGroups },
    created,
    updated,
    visibilityType,
    visibilityValue,
    { Unit.right() },
    { Unit.right() })
