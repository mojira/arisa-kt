package io.github.mojira.arisa.modules

import io.github.mojira.arisa.ExecutionTimeframe
import io.github.mojira.arisa.Shadowban
import io.github.mojira.arisa.utils.mockAttachment
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockUser
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class ShadowbanModuleTest : StringSpec({
    val module = ShadowbanModule()

    val shadowbannedUser = mockUser(name = "shadowbanned")
    val noLongerShadowbannedUser = mockUser(name = "nolongerbanned")

    val timeframe = ExecutionTimeframe(
        lastRunTime = Instant.ofEpochMilli(0),
        currentRunTime = Instant.ofEpochMilli(1000),
        shadowbans = mapOf(
            (
                "shadowbanned" to Shadowban(
                    user = "shadowbanned",
                    since = Instant.ofEpochMilli(500),
                    until = Instant.ofEpochMilli(1500)
                )
            ),
            (
                "nolongerbanned" to Shadowban(
                    user = "nolongerbanned",
                    since = Instant.ofEpochMilli(0),
                    until = Instant.ofEpochMilli(500)
                )
            )
        ),
        openEnded = false
    )

    "should remove bug reports created during shadowban" {
        var reporter = "shadowbanned"
        var isPrivate = false
        var resolution = "Unresolved"

        val issue = mockIssue(
            reporter = shadowbannedUser,
            changeReporter = { reporter = it },
            setPrivate = { isPrivate = true },
            resolveAsInvalid = { resolution = "Invalid" },
            created = Instant.ofEpochMilli(800)
        )

        val result = module(issue, timeframe)
        result.shouldBeRight(ModuleResponse)
        reporter shouldBe "SpamBin"
        isPrivate shouldBe true
        resolution shouldBe "Invalid"
    }

    "should not remove bug reports created outside of shadowban duration" {
        var reporter = "shadowbanned"
        var isPrivate = false
        var resolution = "Unresolved"

        val issue = mockIssue(
            reporter = shadowbannedUser,
            changeReporter = { reporter = it },
            setPrivate = { isPrivate = true },
            resolveAsInvalid = { resolution = "Invalid" },
            created = Instant.ofEpochMilli(200)
        )

        val result = module(issue, timeframe)
        result.shouldBeLeft(OperationNotNeededModuleResponse)
        reporter shouldBe "shadowbanned"
        isPrivate shouldBe false
        resolution shouldBe "Unresolved"
    }

    "should not remove bug reports from unbanned users" {
        var reporter = "nolongerbanned"
        var isPrivate = false
        var resolution = "Unresolved"

        val issue = mockIssue(
            reporter = noLongerShadowbannedUser,
            changeReporter = { reporter = it },
            setPrivate = { isPrivate = true },
            resolveAsInvalid = { resolution = "Invalid" },
            created = Instant.ofEpochMilli(800)
        )

        val result = module(issue, timeframe)
        result.shouldBeLeft(OperationNotNeededModuleResponse)
        reporter shouldBe "nolongerbanned"
        isPrivate shouldBe false
        resolution shouldBe "Unresolved"
    }

    "should remove comments created during shadowban" {
        var commentRestricted = false
        var otherCommentRestricted = false

        val shadowbannedComment = mockComment(
            author = shadowbannedUser,
            created = Instant.ofEpochMilli(800),
            restrict = { commentRestricted = true }
        )

        val otherComment = mockComment(
            created = Instant.ofEpochMilli(900),
            restrict = { otherCommentRestricted = true }
        )

        val issue = mockIssue(
            comments = listOf(shadowbannedComment, otherComment)
        )

        val result = module(issue, timeframe)
        result.shouldBeRight(ModuleResponse)
        commentRestricted shouldBe true
        otherCommentRestricted shouldBe false
    }

    "should not restrict restricted comments" {
        var commentRestricted = false

        val shadowbannedComment = mockComment(
            author = shadowbannedUser,
            created = Instant.ofEpochMilli(800),
            restrict = { commentRestricted = true },
            visibilityType = "group",
            visibilityValue = "staff"
        )

        val issue = mockIssue(
            comments = listOf(shadowbannedComment)
        )

        val result = module(issue, timeframe)
        result.shouldBeLeft(OperationNotNeededModuleResponse)
        commentRestricted shouldBe false
    }

    "should not remove comments created outside of shadowban duration" {
        var commentRestricted = false

        val shadowbannedComment = mockComment(
            author = shadowbannedUser,
            created = Instant.ofEpochMilli(200),
            restrict = { commentRestricted = true }
        )

        val issue = mockIssue(
            comments = listOf(shadowbannedComment)
        )

        val result = module(issue, timeframe)
        result.shouldBeLeft(OperationNotNeededModuleResponse)
        commentRestricted shouldBe false
    }

    "should not remove comments created by unbanned users" {
        var commentRestricted = false

        val shadowbannedComment = mockComment(
            author = noLongerShadowbannedUser,
            created = Instant.ofEpochMilli(800),
            restrict = { commentRestricted = true }
        )

        val issue = mockIssue(
            comments = listOf(shadowbannedComment)
        )

        val result = module(issue, timeframe)
        result.shouldBeLeft(OperationNotNeededModuleResponse)
        commentRestricted shouldBe false
    }

    "should remove attachments created during shadowban" {
        var attachmentRemoved = false
        var otherAttachmentRemoved = false

        val shadowbannedAttachment = mockAttachment(
            uploader = shadowbannedUser,
            created = Instant.ofEpochMilli(800),
            remove = { attachmentRemoved = true }
        )

        val otherAttachment = mockAttachment(
            created = Instant.ofEpochMilli(900),
            remove = { otherAttachmentRemoved = true }
        )

        val issue = mockIssue(
            attachments = listOf(shadowbannedAttachment, otherAttachment)
        )

        val result = module(issue, timeframe)
        result.shouldBeRight(ModuleResponse)
        attachmentRemoved shouldBe true
        otherAttachmentRemoved shouldBe false
    }

    "should not remove attachments created outside of shadowban duration" {
        var attachmentRemoved = false

        val shadowbannedAttachment = mockAttachment(
            uploader = shadowbannedUser,
            created = Instant.ofEpochMilli(200),
            remove = { attachmentRemoved = true }
        )

        val issue = mockIssue(
            attachments = listOf(shadowbannedAttachment)
        )

        val result = module(issue, timeframe)
        result.shouldBeLeft(OperationNotNeededModuleResponse)
        attachmentRemoved shouldBe false
    }

    "should not remove attachments created by unbanned users" {
        var attachmentRemoved = false

        val shadowbannedAttachment = mockAttachment(
            uploader = noLongerShadowbannedUser,
            created = Instant.ofEpochMilli(800),
            remove = { attachmentRemoved = true }
        )

        val issue = mockIssue(
            attachments = listOf(shadowbannedAttachment)
        )

        val result = module(issue, timeframe)
        result.shouldBeLeft(OperationNotNeededModuleResponse)
        attachmentRemoved shouldBe false
    }
})
