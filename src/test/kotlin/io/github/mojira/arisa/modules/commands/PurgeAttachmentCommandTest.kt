package io.github.mojira.arisa.modules.commands

import io.github.mojira.arisa.utils.mockAttachment
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockUser
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

class PurgeAttachmentCommandTest : StringSpec({
    "should purge attachments inside of range" {
        var attachment0Removed = false
        var attachment1Removed = false
        var attachment2Removed = false
        var attachment3Removed = false
        val command = PurgeAttachmentCommand()

        val issue = mockIssue(
            attachments = listOf(
                mockAttachment(
                    id = "0",
                    remove = { attachment0Removed = true },
                    uploader = mockUser(name = "annoyingUser")
                ),
                mockAttachment(
                    id = "1",
                    remove = { attachment1Removed = true },
                    uploader = mockUser(name = "annoyingUser")
                ),
                mockAttachment(
                    id = "2",
                    remove = { attachment2Removed = true },
                    uploader = mockUser(name = "annoyingUser")
                ),
                mockAttachment(
                    id = "3",
                    remove = { attachment3Removed = true },
                    uploader = mockUser(name = "annoyingUser")
                )
            )
        )

        val result = command(issue, "annoyingUser", 1, 2)

        result shouldBe 2
        attachment0Removed.shouldBeFalse()
        attachment1Removed.shouldBeTrue()
        attachment2Removed.shouldBeTrue()
        attachment3Removed.shouldBeFalse()
    }

    "should purge attachments with specified ID or larger" {
        var attachment0Removed = false
        var attachment1Removed = false
        var attachment2Removed = false
        var attachment3Removed = false
        val command = PurgeAttachmentCommand()

        val issue = mockIssue(
            attachments = listOf(
                mockAttachment(
                    id = "0",
                    remove = { attachment0Removed = true },
                    uploader = mockUser(name = "annoyingUser")
                ),
                mockAttachment(
                    id = "1",
                    remove = { attachment1Removed = true },
                    uploader = mockUser(name = "annoyingUser")
                ),
                mockAttachment(
                    id = "2",
                    remove = { attachment2Removed = true },
                    uploader = mockUser(name = "annoyingUser")
                ),
                mockAttachment(
                    id = "3",
                    remove = { attachment3Removed = true },
                    uploader = mockUser(name = "annoyingUser")
                )
            )
        )

        val result = command(issue, "annoyingUser", 2, Int.MAX_VALUE)

        result shouldBe 2
        attachment0Removed.shouldBeFalse()
        attachment1Removed.shouldBeFalse()
        attachment2Removed.shouldBeTrue()
        attachment3Removed.shouldBeTrue()
    }

    "should purge all attachments by user in user argument" {
        var attachment0Removed = false
        var attachment1Removed = false
        var attachment2Removed = false
        val command = PurgeAttachmentCommand()

        val issue = mockIssue(
            attachments = listOf(
                mockAttachment(
                    id = "0",
                    remove = { attachment0Removed = true },
                    uploader = mockUser(name = "annoyingUser")
                ),
                mockAttachment(
                    id = "1",
                    remove = { attachment1Removed = true },
                    uploader = mockUser(name = "someModerator")
                ),
                mockAttachment(
                    id = "2",
                    remove = { attachment2Removed = true },
                    uploader = mockUser(name = "annoyingUser")
                )
            )
        )

        val result = command(issue, "annoyingUser", 0, Int.MAX_VALUE)

        result shouldBe 2
        attachment0Removed.shouldBeTrue()
        attachment1Removed.shouldBeFalse()
        attachment2Removed.shouldBeTrue()
    }
})
