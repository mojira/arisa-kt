package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.User
import io.github.mojira.arisa.utils.NOW
import io.github.mojira.arisa.utils.mockChangeLogItem
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant

private val REPORTER = getUser(name = "reporter")
private val RANDOM_USER = getUser(name = "randomuser")

private val TEN_SECONDS_AGO = NOW.minusSeconds(10)

private val MODULE = ReopenAwaitingModule(
    listOf("staff", "global-moderators"),
    listOf("helper", "staff", "global-moderators"),
    "MEQS_KEEP_AR"
)
private val AWAITING_RESOLVE = mockChangeLogItem(
    created = TEN_SECONDS_AGO,
    field = "resolution",
    changedTo = "Awaiting Response"
)

class ReopenAwaitingModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no resolution" {
        val updated = NOW.plusSeconds(3)
        val issue = mockIssue(
            updated = updated,
            reporter = REPORTER,
            comments = listOf(getComment()),
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is not in awaiting response" {
        val updated = NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Test",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(getComment()),
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket is less than 2 seconds old" {
        val updated = NOW.plusSeconds(1)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(getComment()),
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there are no comments" {
        val updated = NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is a keep AR tag" {
        val updated = NOW.plusSeconds(3)
        val comment = getComment(
            body = "MEQS_KEEP_AR",
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is only a comment from before the resolve" {
        val updated = NOW.plusSeconds(3)
        val comment = getComment(
            NOW.minusSeconds(20),
            NOW.minusSeconds(20)
        )
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there were multiple resolves, but no comment after the last resolve." {
        val updated = NOW.plusSeconds(3)
        val oldResolve = mockChangeLogItem(
            created = NOW.minusSeconds(30),
            field = "resolution",
            changedTo = "Awaiting Response"
        )
        val comment = getComment(
            NOW.minusSeconds(20),
            NOW.minusSeconds(20)
        )
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(oldResolve, AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there were multiple resolves, but no changes after the last resolve." {
        val updated = NOW.plusSeconds(3)
        val oldResolve = mockChangeLogItem(
            created = NOW.minusSeconds(30),
            field = "resolution",
            changedTo = "Awaiting Response"
        )
        val changeLog = mockChangeLogItem(
            created = NOW.minusSeconds(15),
            field = "customfield_00042",
            changedTo = "Confirmed"
        )
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            changeLog = listOf(oldResolve, changeLog, AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when just the comment was updated" {
        val comment = getComment(NOW.plusSeconds(3), NOW.minusSeconds(20))
        val updated = NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            comments = listOf(comment),
            updated = updated,
            reporter = REPORTER,
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment is restricted" {
        val comment = getComment(visibilityType = "group", visibilityValue = "helper")
        val updated = NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            comments = listOf(comment),
            updated = updated,
            reporter = REPORTER,
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comment author is staff" {
        val comment = getComment(authorGroups = listOf("staff"))
        val updated = NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            comments = listOf(comment),
            updated = updated,
            reporter = REPORTER,
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there's no change after resolved" {
        val changeLog = mockChangeLogItem(
            author = REPORTER,
            created = NOW.minusSeconds(20),
            field = "Versions",
            changedTo = "1.15.2"
        )
        val updated = NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            changeLog = listOf(changeLog, AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the author of the change log is not the reporter" {
        val changeLog = mockChangeLogItem(
            created = NOW.plusSeconds(3),
            field = "Versions",
            changedTo = "1.15.2"
        )
        val updated = NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            changeLog = listOf(AWAITING_RESOLVE, changeLog)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the only change log is about comment" {
        val changeLog = mockChangeLogItem(
            created = NOW.plusSeconds(3),
            field = "Comment",
            changedFrom = "aaa",
            changedTo = "AAA",
            author = REPORTER
        ) { emptyList() }
        val updated = NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            changeLog = listOf(AWAITING_RESOLVE, changeLog)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return ModuleResponse when ticket is reopened" {
        val updated = NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(getComment()),
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen even if there is a restricted comment before the good comment" {
        val updated = NOW.plusSeconds(3)
        val commentFail = getComment(visibilityType = "group", visibilityValue = "staff")
        val commentSuccess = getComment()
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(commentSuccess, commentFail),
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen even if there is a restricted comment after the good comment" {
        val updated = NOW.plusSeconds(3)
        val commentFail = getComment(visibilityType = "group", visibilityValue = "staff")
        val commentSuccess = getComment()
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(commentFail, commentSuccess),
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should ignore changes that are not a resolve" {
        val updated = NOW.plusSeconds(3)
        val change = mockChangeLogItem(
            created = NOW.plusSeconds(3),
            field = "",
            changedFrom = "",
            changedTo = "Confirmed"
        ) { emptyList() }
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(getComment()),
            changeLog = listOf(AWAITING_RESOLVE, change)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when someone answered" {
        val updated = NOW.plusSeconds(3)
        val comment = getComment()
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen if the keep AR tag is not restricted" {
        val updated = NOW.plusSeconds(3)
        val comment = getComment(body = "MEQS_KEEP_AR")
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when the comment was both created and updated" {
        val comment = getComment(NOW.plusSeconds(3), NOW.minusSeconds(5))
        val updated = NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment is restricted, but not to a group" {
        val updated = NOW.plusSeconds(3)
        val comment = getComment(visibilityType = "not-a-group", visibilityValue = "helper")
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment is restricted, but not to a blacklisted group" {
        val updated = NOW.plusSeconds(3)
        val comment = getComment(visibilityType = "group", visibilityValue = "users")
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment author has no groups" {
        val updated = NOW.plusSeconds(3)
        val comment = getComment(authorGroups = emptyList())
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when comment author has no blacklisted groups" {
        val updated = NOW.plusSeconds(3)
        val comment = getComment(authorGroups = listOf("Users"))
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when the reporter updated the ticket after being resolved" {
        val changeLog = mockChangeLogItem(
            author = REPORTER,
            created = NOW.plusSeconds(3),
            field = "Versions",
            changedTo = "1.15.2"
        )
        val updated = NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            changeLog = listOf(AWAITING_RESOLVE, changeLog)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should reopen when the ticket is updated by both comments and reporter's changes" {
        val changeLog = mockChangeLogItem(
            author = REPORTER,
            created = NOW.plusSeconds(3),
            field = "Versions",
            changedTo = "1.15.2"
        )
        val comment = getComment()
        val updated = NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE, changeLog)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse with all exceptions when reopening fails" {
        val updated = NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(getComment()),
            changeLog = listOf(AWAITING_RESOLVE)
        ) { RuntimeException().left() }

        val result = MODULE(issue, TEN_SECONDS_AGO)

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
