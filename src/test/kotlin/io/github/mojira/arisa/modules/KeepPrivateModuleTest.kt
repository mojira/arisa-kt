package io.github.mojira.arisa.modules

import arrow.core.right
import io.github.mojira.arisa.utils.PRIVATE_SECURITY_LEVEL
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockChangeLogItem
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private val REMOVE_SECURITY = mockChangeLogItem(
    created = RIGHT_NOW.minusSeconds(10),
    field = "security",
    changedFromString = PRIVATE_SECURITY_LEVEL,
    getAuthorGroups = { listOf("user") }
)

private const val PUBLIC_SECURITY_LEVEL = "public"

private val USE_PUBLIC_SECURITY = mockChangeLogItem(
    created = RIGHT_NOW.minusSeconds(10),
    field = "security",
    changedFromString = PRIVATE_SECURITY_LEVEL,
    changedToString = PUBLIC_SECURITY_LEVEL,
    getAuthorGroups = { listOf("user") }
)

private val REMOVE_SECURITY_STAFF = mockChangeLogItem(
    created = RIGHT_NOW.minusSeconds(10),
    field = "security",
    changedFromString = PRIVATE_SECURITY_LEVEL,
    getAuthorGroups = { listOf("staff") }
)

class KeepPrivateModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when keep private tag is null" {
        val module = KeepPrivateModule(null, "message", setOf(PRIVATE_SECURITY_LEVEL))
        val comment = mockComment(
            body = "MEQS_KEEP_PRIVATE",
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

    "should return OperationNotNeededModuleResponse when comments are empty" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message", setOf(PRIVATE_SECURITY_LEVEL))
        val issue = mockIssue(
            changeLog = listOf(REMOVE_SECURITY)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when no comment contains private tag" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message", setOf(PRIVATE_SECURITY_LEVEL))
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
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message", setOf(PRIVATE_SECURITY_LEVEL))
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
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message", setOf(PRIVATE_SECURITY_LEVEL))
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

    "should both set to private and comment when security level is removed by staff" {
        var didSetToPrivate = false
        var didComment = false

        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message", setOf(PRIVATE_SECURITY_LEVEL))
        val comment = mockComment(
            body = "MEQS_KEEP_PRIVATE",
            created = RIGHT_NOW.minusSeconds(20),
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val issue = mockIssue(
            comments = listOf(comment),
            changeLog = listOf(REMOVE_SECURITY_STAFF),
            setPrivate = { didSetToPrivate = true; Unit.right() },
            addRawRestrictedComment = { _, _ -> didComment = true; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        didSetToPrivate shouldBe true
        didComment shouldBe true
    }

    "should both set to private and comment when security level is null" {
        var didSetToPrivate = false
        var didComment = false

        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message", setOf(PRIVATE_SECURITY_LEVEL))
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

    "should not set to private if bug report is already private but with different security level" {
        var didSetToPrivate = false

        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message", setOf(PRIVATE_SECURITY_LEVEL, "special-private"))
        val comment = mockComment(
            body = "MEQS_KEEP_PRIVATE",
            created = RIGHT_NOW.minusSeconds(20),
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val issue = mockIssue(
            securityLevel = "special-private",
            comments = listOf(comment),
            setPrivate = { didSetToPrivate = true; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
        didSetToPrivate shouldBe false
    }

    "should both set to private and comment when security level is not private" {
        var didSetToPrivate = false
        var didComment = false

        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message", setOf(PRIVATE_SECURITY_LEVEL))
        val comment = mockComment(
            body = "MEQS_KEEP_PRIVATE",
            created = RIGHT_NOW.minusSeconds(20),
            visibilityType = "group",
            visibilityValue = "staff"
        )
        val issue = mockIssue(
            securityLevel = PUBLIC_SECURITY_LEVEL,
            comments = listOf(comment),
            changeLog = listOf(USE_PUBLIC_SECURITY),
            setPrivate = { didSetToPrivate = true; Unit.right() },
            addComment = { didComment = true; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        didSetToPrivate shouldBe true
        didComment shouldBe true
    }

    "should both set to private and comment when security level is transitively not private" {
        var didSetToPrivate = false
        var didComment = false

        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message", setOf(PRIVATE_SECURITY_LEVEL))
        val comment = mockComment(
            body = "MEQS_KEEP_PRIVATE",
            created = RIGHT_NOW.minusSeconds(20),
            visibilityType = "group",
            visibilityValue = "staff"
        )

        val otherPublicLevel = "other-$PUBLIC_SECURITY_LEVEL"
        val otherSecurityLevelChange = mockChangeLogItem(
            created = RIGHT_NOW.minusSeconds(5),
            field = "security",
            changedFromString = PUBLIC_SECURITY_LEVEL,
            changedToString = otherPublicLevel,
            getAuthorGroups = { listOf("user") }
        )

        val issue = mockIssue(
            securityLevel = otherPublicLevel,
            comments = listOf(comment),
            // private -> public -> other-public
            changeLog = listOf(USE_PUBLIC_SECURITY, otherSecurityLevelChange),
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

        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message", setOf(PRIVATE_SECURITY_LEVEL))
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

        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE", "message", setOf(PRIVATE_SECURITY_LEVEL))
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
})
