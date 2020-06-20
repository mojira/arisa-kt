package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockChangeLogItem
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

private val REMOVE_SECURITY = mockChangeLogItem(
    created = RIGHT_NOW.minusSeconds(10),
    field = "security",
    changedFromString = "10318"
)

class KeepPrivateModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when keep private tag is null" {
        val module = KeepPrivateModule(null, "message")
        val comment = mockComment("MEQS_KEEP_PRIVATE", visibilityType = "group", visibilityValue = "staff")
        val issue = mockIssue(
            comments = listOf(comment),
            changeLog = listOf(REMOVE_SECURITY)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comments are empty" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message")
        val issue = mockIssue(
            changeLog = listOf(REMOVE_SECURITY)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when no comment contains private tag" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message")
        val comment = mockComment(
            body = "Hello world!",
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val issue = mockIssue(
            comments = listOf(comment),
            changeLog = listOf(REMOVE_SECURITY)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the comment isn't restricted to staff group" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message")
        val comment = mockComment(
            body = "MEQS_KEEP_PRIVATE"
        )
        val issue = mockIssue(
            comments = listOf(comment),
            changeLog = listOf(REMOVE_SECURITY)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when security level is set to private" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message")
        val comment = mockComment(
            body = "MEQS_KEEP_PRIVATE",
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val issue = mockIssue(
            securityLevel = "private",
            comments = listOf(comment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should both set to private and comment when security level is null" {
        var didSetToPrivate = false
        var didComment = false

        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message")
        val comment = mockComment(
            body = "MEQS_KEEP_PRIVATE",
            created = RIGHT_NOW.minusSeconds(20),
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val issue = mockIssue(
            comments = listOf(comment),
            changeLog = listOf(REMOVE_SECURITY),
            setPrivate = { didSetToPrivate = true; Unit.right() },
            addComment = { didComment = true; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        didSetToPrivate shouldBe true
        didComment shouldBe true
    }

    "should both set to private and comment when security level is not private" {
        var didSetToPrivate = false
        var didComment = false

        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message")
        val comment = mockComment(
            body = "MEQS_KEEP_PRIVATE",
            created = RIGHT_NOW.minusSeconds(20),
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val issue = mockIssue(
            securityLevel = "not private",
            comments = listOf(comment),
            changeLog = listOf(REMOVE_SECURITY),
            setPrivate = { didSetToPrivate = true; Unit.right() },
            addComment = { didComment = true; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        didSetToPrivate shouldBe true
        didComment shouldBe true
    }

    "should set to private but not comment when security level has never been changed" {
        var didSetToPrivate = false
        var didComment = false

        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message")
        val comment = mockComment(
            body = "MEQS_KEEP_PRIVATE",
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val issue = mockIssue(
            comments = listOf(comment),
            setPrivate = { didSetToPrivate = true; Unit.right() },
            addComment = { didComment = true; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        didSetToPrivate shouldBe true
        didComment shouldBe false
    }

    "should set to private but not comment when security level hasn't been changed since the ticket was marked" {
        var didSetToPrivate = false
        var didComment = false

        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message")
        val comment = mockComment(
            body = "MEQS_KEEP_PRIVATE",
            created = RIGHT_NOW.minusSeconds(2),
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val issue = mockIssue(
            comments = listOf(comment),
            setPrivate = { didSetToPrivate = true; Unit.right() },
            addComment = { didComment = true; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        didSetToPrivate shouldBe true
        didComment shouldBe false
    }

    "should return FailedModuleResponse when setting security level fails" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message")
        val comment = mockComment(
            body = "MEQS_KEEP_PRIVATE",
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val issue = mockIssue(
            comments = listOf(comment),
            changeLog = listOf(REMOVE_SECURITY),
            setPrivate = { RuntimeException().left() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when posting comment" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message")
        val comment = mockComment(
            body = "MEQS_KEEP_PRIVATE",
            created = RIGHT_NOW.minusSeconds(20),
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val issue = mockIssue(
            comments = listOf(comment),
            changeLog = listOf(REMOVE_SECURITY),
            addComment = { RuntimeException().left() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
