package io.github.mojira.arisa.modules

import arrow.core.right
import io.github.mojira.arisa.utils.mockAttachment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import me.urielsalis.mccrashlib.CrashReader
import java.time.Instant
import java.time.temporal.ChronoUnit

const val CRASH = """---- Minecraft Crash Report ----
// Surprise! Haha. Well, this is awkward.

Time: 26.06.20 11:23
Description: Unexpected error

java.util.ConcurrentModificationException

-- System Details --
Details:
	Minecraft Version: 1.16.1
	Minecraft Version ID: 1.16.1
	Operating System: Windows 10 (amd64) version 10.0
	Java Version: 1.8.0_51, Oracle Corporation
	Java VM Version: Java HotSpot(TM) 64-Bit Server VM (mixed mode), Oracle Corporation
	Is Modded: Probably not. Jar signature remains and client brand is untouched.
	Type: Client (map_client.txt)"""

private val NOW = Instant.now()
private val A_SECOND_AGO = NOW.minusSeconds(1)

private val crashReader = CrashReader()

class MissingCrashModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when issue has no description" {
        val module = MissingCrashModule(
            listOf("txt"),
            crashReader,
            "crash"
        )
        val issue = mockIssue(
            attachments = emptyList(),
            description = null,
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsAwaitingResponse = { Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)
        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when issue description doesnt contain crash" {
        val module = MissingCrashModule(
            listOf("txt"),
            crashReader,
            "crash"
        )
        val issue = mockIssue(
            attachments = emptyList(),
            description = "Just a normal description",
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsAwaitingResponse = { Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)
        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when issue is Reopened" {
        val module = MissingCrashModule(
            listOf("txt"),
            crashReader,
            "crash"
        )
        val issue = mockIssue(
            status = "Reopened",
            attachments = emptyList(),
            description = "Just a normal description",
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsAwaitingResponse = { Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)
        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when issue has a crash" {
        var resolved = false

        val module = MissingCrashModule(
            listOf("txt"),
            crashReader,
            "crash"
        )

        val attachment = getAttachment(
            content = CRASH,
            created = NOW.minus(42, ChronoUnit.DAYS)
        )

        val issue = mockIssue(
            attachments = listOf(attachment),
            description = "I HaZ crash!",
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsAwaitingResponse = { resolved = true; Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)
        result.shouldBeLeft(OperationNotNeededModuleResponse)
        resolved.shouldBeFalse()
    }

    "should resolve as awaiting response when issue doesnt have a crash" {
        var resolved = false

        val module = MissingCrashModule(
            listOf("txt"),
            crashReader,
            "crash"
        )

        val issue = mockIssue(
            attachments = emptyList(),
            description = "I HaZ Crash!",
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsAwaitingResponse = { resolved = true; Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)
        result.shouldBeRight()
        resolved.shouldBeTrue()
    }

    "should resolve as awaiting response when issue doesnt have a crash ignoring case" {
        var resolved = false

        val module = MissingCrashModule(
            listOf("txt"),
            crashReader,
            "crash"
        )

        val issue = mockIssue(
            attachments = emptyList(),
            description = "I HaZ CrAshEd!",
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsAwaitingResponse = { resolved = true; Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)
        result.shouldBeRight()
        resolved.shouldBeTrue()
    }
})

private fun getAttachment(
    content: String,
    name: String = "crash.txt",
    created: Instant = NOW,
    remove: () -> Unit = { }
) = mockAttachment(
    name = name,
    created = created,
    remove = remove,
    getContent = { content.toByteArray() }
)
