package io.github.mojira.arisa.modules

import arrow.core.right
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockComment
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockLink
import io.github.mojira.arisa.utils.mockLinkedIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private val duplicatesLinkPrivate = mockLink(
    type = "Duplicate",
    issue = mockLinkedIssue(
        getFullIssue = { mockIssue(securityLevel = "private").right() }
    )
)
private val duplicatesLinkPrivateComment = mockLink(
    type = "Duplicate",
    issue = mockLinkedIssue(
        getFullIssue = {
            mockIssue(
                securityLevel = "private",
                comments = listOf(
                    mockComment(
                        body = "MEQS_KEEP_PRIVATE",
                        visibilityType = "group",
                        visibilityValue = "staff"
                    )
                )
            ).right()
        }
    )
)
private val duplicatesLinkPublic = mockLink(
    type = "Duplicate",
    issue = mockLinkedIssue(
        getFullIssue = { mockIssue(securityLevel = null).right() }
    )
)
private val relatesLink = mockLink(
    type = "Relates",
    issue = mockLinkedIssue(
        getFullIssue = { mockIssue(securityLevel = "private").right() }
    )
)

class PrivateDuplicateModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when keep private tag is null" {
        val module = PrivateDuplicateModule(null)
        val issue = mockIssue(
            links = listOf(duplicatesLinkPrivateComment)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when security level is set to private" {
        val module = PrivateDuplicateModule("MEQS_KEEP_PRIVATE")
        val issue = mockIssue(
            securityLevel = "private",
            links = listOf(duplicatesLinkPrivate)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should both set to private and comment when security level is null and parent has comment" {
        var didSetToPrivate = false
        var didComment = false

        val module = PrivateDuplicateModule("MEQS_KEEP_PRIVATE")
        val issue = mockIssue(
            links = listOf(duplicatesLinkPrivateComment),
            setPrivate = { didSetToPrivate = true; Unit.right() },
            addRawRestrictedComment = { _, _ -> didComment = true; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        didSetToPrivate shouldBe true
        didComment shouldBe true
    }

    "should both set to private and comment when security level is not private and parent has comment" {
        var didSetToPrivate = false
        var didComment = false

        val module = PrivateDuplicateModule("MEQS_KEEP_PRIVATE")
        val issue = mockIssue(
            securityLevel = null,
            links = listOf(duplicatesLinkPrivateComment),
            setPrivate = { didSetToPrivate = true; Unit.right() },
            addRawRestrictedComment = { _, _ -> didComment = true; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        didSetToPrivate shouldBe true
        didComment shouldBe true
    }

    "should set to private but not comment when parent has no comment" {
        var didSetToPrivate = false
        var didComment = false

        val module = PrivateDuplicateModule("MEQS_KEEP_PRIVATE")
        val issue = mockIssue(
            links = listOf(duplicatesLinkPrivate),
            setPrivate = { didSetToPrivate = true; Unit.right() },
            addRawRestrictedComment = { _, _ -> didComment = true; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        didSetToPrivate shouldBe true
        didComment shouldBe false
    }

    "should return OperationNotNeededModuleResponse when link is not duplicates" {
        val module = PrivateDuplicateModule("MEQS_KEEP_PRIVATE")
        val issue = mockIssue(
            links = listOf(relatesLink)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when parent is not private" {
        val module = PrivateDuplicateModule("MEQS_KEEP_PRIVATE")
        val issue = mockIssue(
            links = listOf(duplicatesLinkPublic)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }
})
