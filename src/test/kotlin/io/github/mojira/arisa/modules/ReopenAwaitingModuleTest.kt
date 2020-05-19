package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.ChangeLogItem
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.User
import io.github.mojira.arisa.modules.ReopenAwaitingModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant

private val NOW = Instant.now()
private val REPORTER = getUser(name = "reporter")
private val RANDOM_USER = getUser(name = "randomuser")

class ReopenAwaitingModuleTest : StringSpec({
    val MODULE = ReopenAwaitingModule(
        listOf("staff", "global-moderators"),
        listOf("helper", "staff", "global-moderators"),
        "MEQS_KEEP_AR"
    )

    val AWAITING_RESOLVE =
        ChangeLogItem(NOW.minusSeconds(10), "", "", "Awaiting Response", getUser(name = "piston")) { emptyList() }

    "should return OperationNotNeededModuleResponse when there is no resolution" {
        val updated = NOW.plusSeconds(3)
        val request = Request(
            null,
            NOW.minusSeconds(10),
            NOW,
            updated,
            REPORTER,
            listOf(getComment()),
            listOf(AWAITING_RESOLVE)
        ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is not in awaiting response" {
        val updated = NOW.plusSeconds(3)
        val request = Request(
            "Test",
            NOW.minusSeconds(10),
            NOW,
            updated,
            REPORTER,
            listOf(getComment()),
            listOf(AWAITING_RESOLVE)
        ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is less than 2 seconds old" {
        val updated = NOW.plusSeconds(1)
        val request =
            Request(
                "Awaiting Response",
                NOW.minusSeconds(10),
                NOW,
                updated,
                REPORTER,
                listOf(getComment()),
                listOf(AWAITING_RESOLVE)
            ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there are no comments" {
        val updated = NOW.plusSeconds(3)
        val request = Request(
            "Awaiting Response",
            NOW.minusSeconds(10),
            NOW,
            updated,
            REPORTER,
            emptyList(),
            listOf(AWAITING_RESOLVE)
        ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is a keep AR tag" {
        val updated = NOW.plusSeconds(3)
        val comment = getComment(
            body = "MEQS_KEEP_AR",
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val request =
            Request(
                "Awaiting Response",
                NOW.minusSeconds(10),
                NOW,
                updated,
                REPORTER,
                listOf(comment),
                listOf(AWAITING_RESOLVE)
            ) { Unit.right() }

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
            Request(
                "Awaiting Response",
                NOW.minusSeconds(10),
                NOW,
                updated,
                REPORTER,
                listOf(comment),
                listOf(AWAITING_RESOLVE)
            ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there were multiple resolves, but no comment after the last resolve." {
        val updated = NOW.plusSeconds(3)
        val oldResolve = ChangeLogItem(NOW.minusSeconds(30), "", "", "Awaiting Response", RANDOM_USER) { emptyList() }
        val comment = getComment(
            NOW.minusSeconds(20),
            NOW.minusSeconds(20)
        )
        val request = Request(
            "Awaiting Response",
            NOW.minusSeconds(10),
            NOW,
            updated,
            REPORTER,
            listOf(comment),
            listOf(oldResolve, AWAITING_RESOLVE)
        ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there were multiple resolves, but no changes after the last resolve." {
        val updated = NOW.plusSeconds(3)
        val oldResolve = ChangeLogItem(NOW.minusSeconds(30), "", "", "Awaiting Response", RANDOM_USER) { emptyList() }
        val changeLog = ChangeLogItem(NOW.minusSeconds(15), "", "", "Confirmed", RANDOM_USER) { emptyList() }
        val request = Request(
            "Awaiting Response",
            NOW.minusSeconds(10),
            NOW,
            updated,
            REPORTER,
            emptyList(),
            listOf(oldResolve, changeLog, AWAITING_RESOLVE)
        ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when just the comment was updated" {
        val comment = getComment(NOW.plusSeconds(3), NOW.minusSeconds(20))
        val updated = NOW.plusSeconds(3)
        val request =
            Request(
                "Awaiting Response",
                NOW.minusSeconds(10),
                NOW,
                updated,
                REPORTER,
                listOf(comment),
                listOf(AWAITING_RESOLVE)
            ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment is restricted" {
        val comment = getComment(visibilityType = "group", visibilityValue = "helper")
        val updated = NOW.plusSeconds(3)
        val request =
            Request(
                "Awaiting Response",
                NOW.minusSeconds(10),
                NOW,
                updated,
                REPORTER,
                listOf(comment),
                listOf(AWAITING_RESOLVE)
            ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment author is staff" {
        val comment = getComment(authorGroups = listOf("staff"))
        val updated = NOW.plusSeconds(3)
        val request =
            Request(
                "Awaiting Response",
                NOW.minusSeconds(10),
                NOW,
                updated,
                REPORTER,
                listOf(comment),
                listOf(AWAITING_RESOLVE)
            ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there's no change after resolved" {
        val changeLog = ChangeLogItem(
            NOW.minusSeconds(20), "Versions", null, "1.15.2", REPORTER
        ) { emptyList() }
        val updated = NOW.plusSeconds(3)
        val request =
            Request(
                "Awaiting Response",
                NOW.minusSeconds(10),
                NOW,
                updated,
                REPORTER,
                emptyList(),
                listOf(changeLog, AWAITING_RESOLVE)
            ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the author of the change log is not the reporter" {
        val changeLog = ChangeLogItem(
            NOW.plusSeconds(3), "Versions", null, "1.15.2", RANDOM_USER
        ) { emptyList() }
        val updated = NOW.plusSeconds(3)
        val request =
            Request(
                "Awaiting Response",
                NOW.minusSeconds(10),
                NOW,
                updated,
                REPORTER,
                emptyList(),
                listOf(AWAITING_RESOLVE, changeLog)
            ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the only change log is about comment" {
        val changeLog = ChangeLogItem(
            NOW.plusSeconds(3), "Comment", "aaa", "AAA", REPORTER
        ) { emptyList() }
        val updated = NOW.plusSeconds(3)
        val request =
            Request(
                "Awaiting Response",
                NOW.minusSeconds(10),
                NOW,
                updated,
                REPORTER,
                emptyList(),
                listOf(AWAITING_RESOLVE, changeLog)
            ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return ModuleResponse when ticket is reopened" {
        val updated = NOW.plusSeconds(3)
        val request =
            Request(
                "Awaiting Response",
                NOW.minusSeconds(10),
                NOW,
                updated,
                REPORTER,
                listOf(getComment()),
                listOf(AWAITING_RESOLVE)
            ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen even if there is a restricted comment before the good comment" {
        val updated = NOW.plusSeconds(3)
        val commentFail = getComment(visibilityType = "group", visibilityValue = "staff")
        val commentSuccess = getComment()
        val request = Request(
            "Awaiting Response",
            NOW.minusSeconds(10),
            NOW,
            updated,
            REPORTER,
            listOf(commentSuccess, commentFail),
            listOf(AWAITING_RESOLVE)
        ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen even if there is a restricted comment after the good comment" {
        val updated = NOW.plusSeconds(3)
        val commentFail = getComment(visibilityType = "group", visibilityValue = "staff")
        val commentSuccess = getComment()
        val request = Request(
            "Awaiting Response",
            NOW.minusSeconds(10),
            NOW,
            updated,
            REPORTER,
            listOf(commentFail, commentSuccess),
            listOf(AWAITING_RESOLVE)
        ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should ignore changes that are not a resolve" {
        val updated = NOW.plusSeconds(3)
        val change = ChangeLogItem(NOW.plusSeconds(3), "", "", "Confirmed", RANDOM_USER) { emptyList() }
        val request = Request(
            "Awaiting Response",
            NOW.minusSeconds(10),
            NOW,
            updated,
            REPORTER,
            listOf(getComment()),
            listOf(AWAITING_RESOLVE, change)
        ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when someone answered" {
        val updated = NOW.plusSeconds(3)
        val comment = getComment()
        val request =
            Request(
                "Awaiting Response",
                NOW.minusSeconds(10),
                NOW,
                updated,
                REPORTER,
                listOf(comment),
                listOf(AWAITING_RESOLVE)
            ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen if the keep AR tag is not restricted" {
        val updated = NOW.plusSeconds(3)
        val comment = getComment(body = "MEQS_KEEP_AR")
        val request =
            Request(
                "Awaiting Response",
                NOW.minusSeconds(10),
                NOW,
                updated,
                REPORTER,
                listOf(comment),
                listOf(AWAITING_RESOLVE)
            ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when the comment was both created and updated" {
        val comment = getComment(NOW.plusSeconds(3), NOW.minusSeconds(5))
        val updated = NOW.plusSeconds(3)
        val request =
            Request(
                "Awaiting Response",
                NOW.minusSeconds(10),
                NOW,
                updated,
                REPORTER,
                listOf(comment),
                listOf(AWAITING_RESOLVE)
            ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment is restricted, but not to a group" {
        val updated = NOW.plusSeconds(3)
        val comment = getComment(visibilityType = "not-a-group", visibilityValue = "helper")
        val request =
            Request(
                "Awaiting Response",
                NOW.minusSeconds(10),
                NOW,
                updated,
                REPORTER,
                listOf(comment),
                listOf(AWAITING_RESOLVE)
            ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment is restricted, but not to a blacklisted group" {
        val updated = NOW.plusSeconds(3)
        val comment = getComment(visibilityType = "group", visibilityValue = "users")
        val request =
            Request(
                "Awaiting Response",
                NOW.minusSeconds(10),
                NOW,
                updated,
                REPORTER,
                listOf(comment),
                listOf(AWAITING_RESOLVE)
            ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment author has no groups" {
        val updated = NOW.plusSeconds(3)
        val comment = getComment(authorGroups = emptyList())
        val request =
            Request(
                "Awaiting Response",
                NOW.minusSeconds(10),
                NOW,
                updated,
                REPORTER,
                listOf(comment),
                listOf(AWAITING_RESOLVE)
            ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment author has no blacklisted groups" {
        val updated = NOW.plusSeconds(3)
        val comment = getComment(authorGroups = listOf("Users"))
        val request =
            Request(
                "Awaiting Response",
                NOW.minusSeconds(10),
                NOW,
                updated,
                REPORTER,
                listOf(comment),
                listOf(AWAITING_RESOLVE)
            ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when the reporter updated the ticket after being resolved" {
        val changeLog = ChangeLogItem(
            NOW.plusSeconds(3), "Versions", null, "1.15.2", REPORTER
        ) { emptyList() }
        val updated = NOW.plusSeconds(3)
        val request =
            Request(
                "Awaiting Response",
                NOW.minusSeconds(10),
                NOW,
                updated,
                REPORTER,
                emptyList(),
                listOf(AWAITING_RESOLVE, changeLog)
            ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when the ticket is updated by both comments and reporter's changes" {
        val changeLog = ChangeLogItem(
            NOW.plusSeconds(3), "Versions", null, "1.15.2", REPORTER
        ) { emptyList() }
        val comment = getComment()
        val updated = NOW.plusSeconds(3)
        val request =
            Request(
                "Awaiting Response",
                NOW.minusSeconds(10),
                NOW,
                updated,
                REPORTER,
                listOf(comment),
                listOf(AWAITING_RESOLVE, changeLog)
            ) { Unit.right() }

        val result = MODULE(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse with all exceptions when reopening fails" {
        val updated = NOW.plusSeconds(3)
        val request = Request(
            "Awaiting Response",
            NOW.minusSeconds(10),
            NOW,
            updated,
            REPORTER,
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
    body: String = "",
    visibilityType: String? = null,
    visibilityValue: String? = null,
    author: User = RANDOM_USER,
    authorGroups: List<String>? = null
) = Comment(
    body,
    author,
    { authorGroups },
    created,
    updated,
    visibilityType,
    visibilityValue,
    { Unit.right() },
    { Unit.right() }
)

private fun getUser(name: String) = User(name, "User")
