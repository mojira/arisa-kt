package io.github.mojira.arisa.modules

import arrow.core.right
import io.github.mojira.arisa.domain.User
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockChangeLogItem
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockUser
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.temporal.ChronoUnit

private val REPORTER = getUser(name = "reporter")
private val ARISA = getUser(name = "arisabot")
private val RANDOM_USER = getUser(name = "randomUser")
private val NEWBIE = getUser(name = "newbieUser", newUser = true)

private val TEN_SECONDS_AGO = RIGHT_NOW.minusSeconds(10)
private val TWO_YEARS_AGO = RIGHT_NOW.minus(730, ChronoUnit.DAYS)

private const val NOT_REOPEN_AR_MESSAGE = "This report is currently missing crucial information. " +
    "Please take a look at the other comments to find out what we are looking for."

private val MODULE = ReopenAwaitingModule(
    setOf("staff", "global-moderators"),
    setOf("helper", "staff", "global-moderators"),
    365,
    "MEQS_KEEP_AR",
    "ARISA_REOPEN_OP",
    NOT_REOPEN_AR_MESSAGE
)
private val AWAITING_RESOLVE = mockChangeLogItem(
    created = TEN_SECONDS_AGO,
    field = "resolution",
    changedToString = "Awaiting Response"
)
private val OLD_AWAITING_RESOLVE = mockChangeLogItem(
    created = TWO_YEARS_AGO,
    field = "resolution",
    changedToString = "Awaiting Response"
)

class ReopenAwaitingModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no resolution" {
        val updated = RIGHT_NOW.plusSeconds(3)
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
        val updated = RIGHT_NOW.plusSeconds(3)
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
        val updated = RIGHT_NOW.plusSeconds(1)
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
        val updated = RIGHT_NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            changeLog = listOf(AWAITING_RESOLVE)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is only a comment from before the resolve" {
        val updated = RIGHT_NOW.plusSeconds(3)
        val comment = getComment(
            RIGHT_NOW.minusSeconds(20),
            RIGHT_NOW.minusSeconds(20)
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
        val updated = RIGHT_NOW.plusSeconds(3)
        val oldResolve = mockChangeLogItem(
            created = RIGHT_NOW.minusSeconds(30),
            field = "resolution",
            changedToString = "Awaiting Response"
        )
        val comment = getComment(
            RIGHT_NOW.minusSeconds(20),
            RIGHT_NOW.minusSeconds(20)
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
        val updated = RIGHT_NOW.plusSeconds(3)
        val oldResolve = mockChangeLogItem(
            created = RIGHT_NOW.minusSeconds(30),
            field = "resolution",
            changedToString = "Awaiting Response"
        )
        val changeLog = mockChangeLogItem(
            created = RIGHT_NOW.minusSeconds(15),
            field = "customfield_00042",
            changedToString = "Confirmed"
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
        val comment = getComment(RIGHT_NOW.plusSeconds(3), RIGHT_NOW.minusSeconds(20))
        val updated = RIGHT_NOW.plusSeconds(3)
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
        val updated = RIGHT_NOW.plusSeconds(3)
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
        val updated = RIGHT_NOW.plusSeconds(3)
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
            created = RIGHT_NOW.minusSeconds(20),
            field = "Versions",
            changedToString = "1.15.2",
            author = REPORTER
        )
        val updated = RIGHT_NOW.plusSeconds(3)
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
            created = RIGHT_NOW.plusSeconds(3),
            field = "Versions",
            changedToString = "1.15.2"
        )
        val updated = RIGHT_NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            changeLog = listOf(AWAITING_RESOLVE, changeLog)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the author of the change log is arisa" {
        val changeLog = mockChangeLogItem(
            created = RIGHT_NOW.plusSeconds(3),
            field = "Versions",
            changedToString = "1.15.2"
        )
        val updated = RIGHT_NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = ARISA,
            changeLog = listOf(AWAITING_RESOLVE, changeLog)
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the only change log is about comment" {
        val changeLog = mockChangeLogItem(
            created = RIGHT_NOW.plusSeconds(3),
            field = "Comment",
            changedFromString = "aaa",
            changedToString = "AAA",
            author = REPORTER
        ) { emptyList() }
        val updated = RIGHT_NOW.plusSeconds(3)
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
        var hasReopened = false
        var hasCommented = false

        val updated = RIGHT_NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(getComment()),
            changeLog = listOf(AWAITING_RESOLVE),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe true
        hasCommented shouldBe false
    }

    "should reopen even if there is a restricted comment before the good comment" {
        var hasReopened = false
        var hasCommented = false

        val updated = RIGHT_NOW.plusSeconds(3)
        val commentFail = getComment(visibilityType = "group", visibilityValue = "staff")
        val commentSuccess = getComment()
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(commentSuccess, commentFail),
            changeLog = listOf(AWAITING_RESOLVE),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe true
        hasCommented shouldBe false
    }

    "should reopen even if there is a restricted comment after the good comment" {
        var hasReopened = false
        var hasCommented = false

        val updated = RIGHT_NOW.plusSeconds(3)
        val commentFail = getComment(visibilityType = "group", visibilityValue = "staff")
        val commentSuccess = getComment()
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(commentFail, commentSuccess),
            changeLog = listOf(AWAITING_RESOLVE),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe true
        hasCommented shouldBe false
    }

    "should ignore changes that are not a resolve" {
        var hasReopened = false
        var hasCommented = false

        val updated = RIGHT_NOW.plusSeconds(3)
        val change = mockChangeLogItem(
            created = RIGHT_NOW.plusSeconds(3),
            field = "",
            changedFromString = "",
            changedToString = "Confirmed"
        ) { emptyList() }
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(getComment()),
            changeLog = listOf(AWAITING_RESOLVE, change),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe true
        hasCommented shouldBe false
    }

    "should reopen when someone answered" {
        var hasReopened = false
        var hasCommented = false

        val updated = RIGHT_NOW.plusSeconds(3)
        val comment = getComment()
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe true
        hasCommented shouldBe false
    }

    "should not reopen when someone answered and only op tag is set" {
        var reopen = false
        val updated = RIGHT_NOW.plusSeconds(3)
        val comment = getComment()
        val keep = getComment(body = "ARISA_REOPEN_OP", visibilityType = "group", visibilityValue = "staff")
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(keep, comment),
            changeLog = listOf(AWAITING_RESOLVE),
            reopen = { reopen = true; Unit.right() },
            addComment = { Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        reopen shouldBe false
    }

    "should reopen when op answered and only op tag is set" {
        var reopen = false
        val updated = RIGHT_NOW.plusSeconds(3)
        val comment = getComment(author = REPORTER)
        val keep = getComment(body = "ARISA_REOPEN_OP", visibilityType = "group", visibilityValue = "staff")
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(keep, comment),
            changeLog = listOf(AWAITING_RESOLVE),
            reopen = { reopen = true; Unit.right() },
            addComment = { Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        reopen shouldBe true
    }

    "should reopen when op updated the bug report, even if only op tag is set" {
        var hasReopened = false
        var hasCommented = false

        val changeLog = mockChangeLogItem(
            created = RIGHT_NOW.plusSeconds(3),
            field = "Versions",
            changedToString = "1.15.2",
            author = REPORTER
        )
        val reopenOpComment = getComment(body = "ARISA_REOPEN_OP", visibilityType = "group", visibilityValue = "staff")
        val updated = RIGHT_NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(reopenOpComment),
            changeLog = listOf(AWAITING_RESOLVE, changeLog),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe true
        hasCommented shouldBe false
    }

    "should reopen when someone answered within the soft AR period" {
        var hasReopened = false
        var hasCommented = false

        val updated = RIGHT_NOW.plusSeconds(3)
        val comment = getComment()
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe true
        hasCommented shouldBe false
    }

    "should reopen when reporter answered after the soft AR period" {
        var hasReopened = false
        var hasCommented = false

        val updated = RIGHT_NOW.plusSeconds(3)
        val comment = getComment(
            author = REPORTER
        )
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(OLD_AWAITING_RESOLVE),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe true
        hasCommented shouldBe false
    }

    "should reopen if the keep AR tag is not restricted" {
        var hasReopened = false
        var hasCommented = false

        val updated = RIGHT_NOW.plusSeconds(3)
        val comment = getComment(body = "MEQS_KEEP_AR")
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe true
        hasCommented shouldBe false
    }

    "should reopen when the comment was both created and updated" {
        var hasReopened = false
        var hasCommented = false

        val comment = getComment(RIGHT_NOW.plusSeconds(3), RIGHT_NOW.minusSeconds(5))
        val updated = RIGHT_NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe true
        hasCommented shouldBe false
    }

    "should reopen when comment is restricted, but not to a group" {
        var hasReopened = false
        var hasCommented = false

        val updated = RIGHT_NOW.plusSeconds(3)
        val comment = getComment(visibilityType = "not-a-group", visibilityValue = "helper")
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe true
        hasCommented shouldBe false
    }

    "should reopen when comment is restricted, but not to a blacklisted group" {
        var hasReopened = false
        var hasCommented = false

        val updated = RIGHT_NOW.plusSeconds(3)
        val comment = getComment(visibilityType = "group", visibilityValue = "users")
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe true
        hasCommented shouldBe false
    }

    "should reopen when comment author has no groups" {
        var hasReopened = false
        var hasCommented = false

        val updated = RIGHT_NOW.plusSeconds(3)
        val comment = getComment(authorGroups = emptyList())
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe true
        hasCommented shouldBe false
    }

    "should reopen when comment author has no blacklisted groups" {
        var hasReopened = false
        var hasCommented = false

        val updated = RIGHT_NOW.plusSeconds(3)
        val comment = getComment(authorGroups = listOf("Users"))
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe true
        hasCommented shouldBe false
    }

    "should reopen when the reporter updated the ticket after being resolved" {
        var hasReopened = false
        var hasCommented = false

        val changeLog = mockChangeLogItem(
            created = RIGHT_NOW.plusSeconds(3),
            field = "Versions",
            changedToString = "1.15.2",
            author = REPORTER
        )
        val updated = RIGHT_NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            changeLog = listOf(AWAITING_RESOLVE, changeLog),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe true
        hasCommented shouldBe false
    }

    "should reopen when the ticket is updated by both comments and reporter's changes" {
        var hasReopened = false
        var hasCommented = false

        val changeLog = mockChangeLogItem(
            created = RIGHT_NOW.plusSeconds(3),
            field = "Versions",
            changedToString = "1.15.2",
            author = REPORTER
        )
        val comment = getComment()
        val updated = RIGHT_NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE, changeLog),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe true
        hasCommented shouldBe false
    }

    "should not reopen when the commenter is a new user" {
        var hasReopened = false
        var hasCommented = false

        val comment = getComment(author = NEWBIE)
        val updated = RIGHT_NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
        hasReopened shouldBe false
        hasCommented shouldBe false
    }

    "should reopen when the commenter is a new user but also the reporter" {
        var hasReopened = false
        var hasCommented = false

        val comment = getComment(author = NEWBIE)
        val updated = RIGHT_NOW.plusSeconds(3)
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = NEWBIE,
            comments = listOf(comment),
            changeLog = listOf(AWAITING_RESOLVE),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe true
        hasCommented shouldBe false
    }

    "should comment the message when there is a keep AR tag" {
        var hasReopened = false
        var hasCommented = false

        val updated = RIGHT_NOW.plusSeconds(3)
        val tagComment = getComment(
            body = "MEQS_KEEP_AR",
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val normalComment = getComment()
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(tagComment, normalComment),
            changeLog = listOf(AWAITING_RESOLVE),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() },
            addRawBotComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe false
        hasCommented shouldBe true
    }

    "should comment the message when someone answered after the soft AR period" {
        var hasReopened = false
        var hasCommented = false

        val updated = RIGHT_NOW.plusSeconds(3)
        val comment = getComment()
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(comment),
            changeLog = listOf(OLD_AWAITING_RESOLVE),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() },
            addRawBotComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe false
        hasCommented shouldBe true
    }

    "should not comment the message when there is a keep AR tag and arisa has already commented" {
        var hasReopened = false
        var hasCommented = false

        val updated = RIGHT_NOW.plusSeconds(3)
        val tagComment = getComment(
            body = "MEQS_KEEP_AR",
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val arisaComment = getComment(
            body = NOT_REOPEN_AR_MESSAGE,
            author = ARISA
        )
        val normalComment = getComment()
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(tagComment, arisaComment, normalComment),
            changeLog = listOf(AWAITING_RESOLVE),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe false
        hasCommented shouldBe false
    }

    "should comment the message when there is a keep AR tag and another user has already commented the message" {
        var hasReopened = false
        var hasCommented = false

        val updated = RIGHT_NOW.plusSeconds(3)
        val tagComment = getComment(
            body = "MEQS_KEEP_AR",
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val fakeComment = getComment(
            body = NOT_REOPEN_AR_MESSAGE,
            author = RANDOM_USER
        )
        val issue = mockIssue(
            resolution = "Awaiting Response",
            updated = updated,
            reporter = REPORTER,
            comments = listOf(tagComment, fakeComment),
            changeLog = listOf(AWAITING_RESOLVE),
            reopen = { hasReopened = true; Unit.right() },
            addComment = { hasCommented = true; Unit.right() },
            addRawBotComment = { hasCommented = true; Unit.right() }
        )

        val result = MODULE(issue, TEN_SECONDS_AGO)

        result.shouldBeRight(ModuleResponse)
        hasReopened shouldBe false
        hasCommented shouldBe true
    }
})

private fun getComment(
    updated: Instant = RIGHT_NOW,
    created: Instant = RIGHT_NOW,
    body: String = "",
    visibilityType: String? = null,
    visibilityValue: String? = null,
    author: User = RANDOM_USER,
    authorGroups: List<String> = emptyList()
) = mockComment(
    body = body,
    author = author,
    getAuthorGroups = { authorGroups },
    created = created,
    updated = updated,
    visibilityType = visibilityType,
    visibilityValue = visibilityValue
)

private fun getUser(name: String, newUser: Boolean = false) =
    mockUser(name = name, displayName = "User", isNewUser = { newUser })
