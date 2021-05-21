package io.github.mojira.arisa.modules

import arrow.core.right
import com.urielsalis.mccrashlib.CrashReader
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.infrastructure.config.CrashDupeConfig
import io.github.mojira.arisa.utils.mockAttachment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.temporal.ChronoUnit

const val EXAMPLE_CRASH = """---- Minecraft Crash Report ----
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

const val PIXEL_FORMAT_CRASH = """---- Minecraft Crash Report ----
// Daisy, daisy...

Time: 6/28/17 11:44 AM
Description: Initializing game

org.lwjgl.LWJGLException: Pixel format not accelerated

-- System Details --
Details:
	Minecraft Version: 1.8.9
	Java Version: 1.8.0_131, Oracle Corporation
Is Modded: Probably not. Jar signature remains and client brand is untouched.
"""

const val UNKNOWN_MODDED_CRASH = """---- Minecraft Crash Report ----
// Oh - I know what I did wrong!

Time: 6/5/18 9:20 PM
Description: Exception generating new chunk

java.util.concurrent.ExecutionException: java.lang.RuntimeException: We are asking a region for a chunk out of bound | -174 8

-- System Details --
Details:
	Minecraft Version: 1.13-pre1
	Operating System: Linux (amd64) version 4.4.0-98-generic
	Java Version: 1.8.0_151, Oracle Corporation
	Java VM Version: Java HotSpot(TM) 64-Bit Server VM (mixed mode), Oracle Corporation
	Memory: 614132352 bytes (585 MB) / 988282880 bytes (942 MB) up to 3340763136 bytes (3186 MB)
	JVM Flags: 2 total; -Xmx3584M -XX:MaxPermSize=256M
	Profiler Position: N/A (disabled)
	Player Count: 1 / 20; [so['CENSORED'/351, l='Vanilla', x=8.50, y=72.00, z=121.50]]
	Data Packs: vanilla
	Is Modded: Unknown (can't tell)
	Type: Dedicated Server (map_server.txt)
"""

const val DEFINITELY_MODDED_SERVER_CRASH = """---- Minecraft Crash Report ----
// Oh - I know what I did wrong!

Time: 6/5/18 9:20 PM
Description: Exception generating new chunk

java.util.concurrent.ExecutionException: java.lang.RuntimeException: We are asking a region for a chunk out of bound | -174 8

-- System Details --
Details:
	Minecraft Version: 1.13-pre1
	Operating System: Linux (amd64) version 4.4.0-98-generic
	Java Version: 1.8.0_151, Oracle Corporation
	Java VM Version: Java HotSpot(TM) 64-Bit Server VM (mixed mode), Oracle Corporation
	Memory: 614132352 bytes (585 MB) / 988282880 bytes (942 MB) up to 3340763136 bytes (3186 MB)
	JVM Flags: 2 total; -Xmx3584M -XX:MaxPermSize=256M
	Profiler Position: N/A (disabled)
	Player Count: 1 / 20; [so['CENSORED'/351, l='Vanilla', x=8.50, y=72.00, z=121.50]]
	Data Packs: vanilla
	Is Modded: Definitely; Server brand changed to 'fabric'
	Type: Dedicated Server (map_server.txt)
"""

const val VERY_LIKELY_MODDED_CRASH = """---- Minecraft Crash Report ----
// Surprise! Haha. Well, this is awkward.

Time: 04.10.19 17:39
Description: Exception ticking world

java.lang.NoSuchFieldError: DO_DAYLIGHT_CYCLE

-- System Details --
Details:
	Minecraft Version: 1.14.4
	Minecraft Version ID: 1.14.4
	Operating System: Windows 10 (amd64) version 10.0
	Java Version: 1.8.0_51, Oracle Corporation
	Java VM Version: Java HotSpot(TM) 64-Bit Server VM (mixed mode), Oracle Corporation
	Memory: 1192941208 bytes (1137 MB) / 1845493760 bytes (1760 MB) up to 2147483648 bytes (2048 MB)
	CPUs: 4
	JVM Flags: 9 total; -XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump -Xss1M -Xmx2G -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M
	Player Count: 0 / 8; []
	Data Packs: vanilla
	Type: Integrated Server (map_client.txt)
	Is Modded: Very likely; Jar signature invalidated
"""

const val DRIVER_NO_OPENGL = """---- Minecraft Crash Report ----
// I feel sad now :(

Time: 12/04/18 18:49
Description: Initializing game

java.lang.IllegalStateException: GLFW error 65542: WGL: The driver does not appear to support OpenGL

-- System Details --
Details:
	Minecraft Version: 18w15a
	Java Version: 1.8.0_25, Oracle Corporation
	Is Modded: Probably not. Jar signature remains and client brand is untouched.
"""

const val DEFINITELY_MODDED_CLIENT_CRASH = """---- Minecraft Crash Report ----
// You're mean.

Time: 06.11.19 10:54
Description: mouseClicked event handler

java.lang.NullPointerException: mouseClicked event handler
	at com.replaymod.simplepathing.ReplayModSimplePathing.onReplayClosing(ReplayModSimplePathing.java:129)

-- System Details --
Details:
	Minecraft Version: 1.14.4
	Minecraft Version ID: 1.14.4
	Operating System: Windows 10 (amd64) version 10.0
	Java Version: 1.8.0_51, Oracle Corporation
	Java VM Version: Java HotSpot(TM) 64-Bit Server VM (mixed mode), Oracle Corporation
	Memory: 1012922624 bytes (965 MB) / 1845493760 bytes (1760 MB) up to 2147483648 bytes (2048 MB)
	Fabric Mods: 
....
	Is Modded: Definitely; Client brand changed to 'fabric'
"""

const val JAVA_CRASH = """#
# A fatal error has been detected by the Java Runtime Environment:
#
#  EXCEPTION_ACCESS_VIOLATION (0xc0000005) at pc=0x000000000c1c1c82, pid=2768, tid=2780
#
# JRE version: 7.0_25-b17
# Java VM: Java HotSpot(TM) 64-Bit Server VM (23.25-b01 mixed mode windows-amd64 compressed oops)
# Problematic frame:
# C  [ig75icd64.dll+0x1c82]
#
# Failed to write core dump. Minidumps are not enabled by default on client versions of Windows
#
# If you would like to submit a bug report, please visit:
#   http://bugreport.sun.com/bugreport/crash.jsp
# The crash happened outside the Java Virtual Machine in native code.
# See problematic frame for where to report the bug.
#"""

private val NOW = Instant.now()
private val A_SECOND_AGO = NOW.minusSeconds(1)

const val Unconfirmed = "Unconfirmed"
val NoPriority = null

private val crashReader = CrashReader()

class CrashModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when issue does not contain any valid crash report" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )
        val issue = mockIssue(
            attachments = emptyList(),
            description = "Help\nmy\ngame\nis\nsuper\nbroken!!\n!!!",
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsInvalid = { Unit.right() },
            resolveAsDuplicate = { Unit.right() },
            createLink = { _, _, _ -> Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when issue body does not contain any recent crash" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )

        val issue = mockIssue(
            attachments = emptyList(),
            description = PIXEL_FORMAT_CRASH,
            created = NOW.minus(42, ChronoUnit.DAYS),
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsInvalid = { Unit.right() },
            resolveAsDuplicate = { Unit.right() },
            createLink = { _, _, _ -> Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when crash configurations are empty and report is not modded" {
        val module = CrashModule(
            listOf("txt"),
            emptyList(),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )
        val issue = mockIssue(
            attachments = emptyList(),
            description = PIXEL_FORMAT_CRASH,
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsInvalid = { Unit.right() },
            resolveAsDuplicate = { Unit.right() },
            createLink = { _, _, _ -> Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when crash configuration has an invalid type and crash is not modded" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("hytale", "The game has not yet been released", "HT-1")),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )

        val issue = mockIssue(
            attachments = emptyList(),
            description = PIXEL_FORMAT_CRASH,
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsInvalid = { Unit.right() },
            resolveAsDuplicate = { Unit.right() },
            createLink = { _, _, _ -> Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when reported crash is not configured and not modded" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Unexpected loophole in Redstone implementation", "MC-108")),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )

        val issue = mockIssue(
            attachments = emptyList(),
            description = PIXEL_FORMAT_CRASH,
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsInvalid = { Unit.right() },
            resolveAsDuplicate = { Unit.right() },
            createLink = { _, _, _ -> Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when issue does not contain any recent crash as attachment" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )

        val attachment = getAttachment(
            content = PIXEL_FORMAT_CRASH,
            created = NOW.minus(42, ChronoUnit.DAYS)
        )

        val issue = mockIssue(
            attachments = listOf(attachment),
            description = "",
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsInvalid = { Unit.right() },
            resolveAsDuplicate = { Unit.right() },
            createLink = { _, _, _ -> Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)
        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when attached crash is not configured and not modded" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Unexpected loophole in Redstone implementation", "MC-108")),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )

        val attachment = getAttachment(
            content = PIXEL_FORMAT_CRASH
        )
        val issue = mockIssue(
            attachments = listOf(attachment),
            description = "",
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsInvalid = { Unit.right() },
            resolveAsDuplicate = { Unit.right() },
            createLink = { _, _, _ -> Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when attached crash does have a wrong mime type" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )

        val attachment = getAttachment(
            name = "crash.png",
            content = PIXEL_FORMAT_CRASH
        )
        val issue = mockIssue(
            attachments = listOf(attachment),
            description = "",
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsInvalid = { Unit.right() },
            resolveAsDuplicate = { Unit.right() },
            createLink = { _, _, _ -> Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when attached crash is not modded (Unknown)" {
        val module = CrashModule(
            listOf("txt"),
            listOf(),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )

        val attachment = getAttachment(
            content = UNKNOWN_MODDED_CRASH
        )
        val issue = mockIssue(
            attachments = listOf(attachment),
            description = "",
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsInvalid = { Unit.right() },
            resolveAsDuplicate = { Unit.right() },
            createLink = { _, _, _ -> Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is both a modded crash and an unmodded one" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )

        val moddedCrash = getAttachment(
            content = DEFINITELY_MODDED_SERVER_CRASH
        )
        val unmoddedCrash = getAttachment(
            content = EXAMPLE_CRASH,
            created = NOW.minusSeconds(42)
        )
        val issue = mockIssue(
            attachments = listOf(moddedCrash, unmoddedCrash),
            description = "",
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsInvalid = { Unit.right() },
            resolveAsDuplicate = { Unit.right() },
            createLink = { _, _, _ -> Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is both a duped crash and one that should not get resolved" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )
        val pixelFormatCrash = getAttachment(
            content = PIXEL_FORMAT_CRASH
        )
        val unmoddedCrash = getAttachment(
            content = EXAMPLE_CRASH,
            created = NOW.minusSeconds(42)
        )
        val issue = mockIssue(
            attachments = listOf(pixelFormatCrash, unmoddedCrash),
            description = "",
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsInvalid = { Unit.right() },
            resolveAsDuplicate = { Unit.right() },
            createLink = { _, _, _ -> Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid when reported server crash is modded" {
        var resolvedAsDupe = false
        var resolvedAsInvalid = false
        var addedAttachment = false
        var addedComment = CommentOptions("")

        val module = CrashModule(
            listOf("txt"),
            emptyList(),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )
        val issue = mockIssue(
            attachments = emptyList(),
            description = DEFINITELY_MODDED_SERVER_CRASH,
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsDuplicate = { resolvedAsDupe = true },
            resolveAsInvalid = { resolvedAsInvalid = true },
            addComment = { addedComment = it },
            addAttachment = { _, cleanupCallback ->
                addedAttachment = true
                cleanupCallback()
            }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeFalse()
        addedAttachment.shouldBeFalse()
        resolvedAsInvalid.shouldBeTrue()
        addedComment shouldBe CommentOptions("modified-game")
    }

    "should resolve as invalid when reported server crash is very likely modded" {
        var resolvedAsDupe = false
        var resolvedAsInvalid = false
        var addedAttachment = false
        var addedComment = CommentOptions("")

        val module = CrashModule(
            listOf("txt"),
            emptyList(),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )
        val issue = mockIssue(
            attachments = emptyList(),
            description = VERY_LIKELY_MODDED_CRASH,
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsDuplicate = { resolvedAsDupe = true },
            resolveAsInvalid = { resolvedAsInvalid = true },
            addComment = { addedComment = it },
            addAttachment = { _, cleanupCallback ->
                addedAttachment = true
                cleanupCallback()
            }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeFalse()
        addedAttachment.shouldBeFalse()
        resolvedAsInvalid.shouldBeTrue()
        addedComment shouldBe CommentOptions("modified-game")
    }

    "should resolve as invalid when reported crash is modded" {
        var resolvedAsDupe = false
        var resolvedAsInvalid = false
        var addedAttachment = false
        var addedComment = CommentOptions("")

        val module = CrashModule(
            listOf("txt"),
            emptyList(),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )
        val issue = mockIssue(
            attachments = emptyList(),
            description = DEFINITELY_MODDED_CLIENT_CRASH,
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsDuplicate = { resolvedAsDupe = true },
            resolveAsInvalid = { resolvedAsInvalid = true },
            addComment = { addedComment = it },
            addAttachment = { _, cleanupCallback ->
                addedAttachment = true
                cleanupCallback()
            }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeFalse()
        addedAttachment.shouldBeFalse()
        resolvedAsInvalid.shouldBeTrue()
        addedComment shouldBe CommentOptions("modified-game")
    }

    "should resolve as invalid when attached crash is modded" {
        var resolvedAsDupe = false
        var resolvedAsInvalid = false
        var addedAttachment = false
        var addedComment = CommentOptions("")

        val module = CrashModule(
            listOf("txt"),
            emptyList(),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )

        val attachment = getAttachment(
            content = DEFINITELY_MODDED_CLIENT_CRASH
        )
        val issue = mockIssue(
            attachments = listOf(attachment),
            description = "",
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsDuplicate = { resolvedAsDupe = true },
            resolveAsInvalid = { resolvedAsInvalid = true },
            addComment = { addedComment = it },
            addAttachment = { _, cleanupCallback ->
                addedAttachment = true
                cleanupCallback()
            }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeFalse()
        addedAttachment.shouldBeFalse()
        resolvedAsInvalid.shouldBeTrue()
        addedComment shouldBe CommentOptions("modified-game")
    }

    "should resolve as dupe when reported crash is in configured list" {
        var resolvedAsDupe = false
        var resolvedAsInvalid = false
        var addedAttachment = false
        var addedComment = CommentOptions("")

        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )
        val issue = mockIssue(
            attachments = emptyList(),
            description = PIXEL_FORMAT_CRASH,
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsDuplicate = { resolvedAsDupe = true },
            resolveAsInvalid = { resolvedAsInvalid = true },
            addComment = { addedComment = it },
            addAttachment = { _, cleanupCallback ->
                addedAttachment = true
                cleanupCallback()
            }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeTrue()
        addedAttachment.shouldBeFalse()
        resolvedAsInvalid.shouldBeFalse()
        addedComment shouldBe CommentOptions("duplicate-tech", "MC-297")
    }

    "should resolve as dupe when attached crash is in configured list" {
        var resolvedAsDupe = false
        var resolvedAsInvalid = false
        var addedAttachment = false
        var addedComment = CommentOptions("")

        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )

        val attachment = getAttachment(
            content = PIXEL_FORMAT_CRASH
        )
        val issue = mockIssue(
            attachments = listOf(attachment),
            description = "",
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsDuplicate = { resolvedAsDupe = true },
            resolveAsInvalid = { resolvedAsInvalid = true },
            addComment = { addedComment = it },
            addAttachment = { _, cleanupCallback ->
                addedAttachment = true
                cleanupCallback()
            }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeTrue()
        addedAttachment.shouldBeFalse()
        resolvedAsInvalid.shouldBeFalse()
        addedComment shouldBe CommentOptions("duplicate-tech", "MC-297")
    }

    "should resolve as dupe when the configured crash is a java crash" {
        var resolvedAsDupe = false
        var resolvedAsInvalid = false
        var addedAttachment = false
        var addedComment = CommentOptions("")

        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("java", "ig75icd64\\.dll", "MC-32606")),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )
        val issue = mockIssue(
            attachments = emptyList(),
            description = JAVA_CRASH,
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsDuplicate = { resolvedAsDupe = true },
            resolveAsInvalid = { resolvedAsInvalid = true },
            addComment = { addedComment = it },
            addAttachment = { _, cleanupCallback ->
                addedAttachment = true
                cleanupCallback()
            }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeTrue()
        addedAttachment.shouldBeFalse()
        resolvedAsInvalid.shouldBeFalse()
        addedComment shouldBe CommentOptions("duplicate-tech", "MC-32606")
    }

    "should resolve as dupe when the configured crash uses regex" {
        var resolvedAsDupe = false
        var resolvedAsInvalid = false
        var addedAttachment = false
        var addedComment = CommentOptions("")

        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("java", "ig[0-9]{1,2}icd[0-9]{2}\\.dll", "MC-32606")),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )
        val issue = mockIssue(
            attachments = emptyList(),
            description = JAVA_CRASH,
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsDuplicate = { resolvedAsDupe = true },
            resolveAsInvalid = { resolvedAsInvalid = true },
            addComment = { addedComment = it },
            addAttachment = { _, cleanupCallback ->
                addedAttachment = true
                cleanupCallback()
            }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeTrue()
        addedAttachment.shouldBeFalse()
        resolvedAsInvalid.shouldBeFalse()
        addedComment shouldBe CommentOptions("duplicate-tech", "MC-32606")
    }

    "should link to configured ticket when resolving as dupe" {
        var resolvedAsDupe = false
        var resolvedAsInvalid = false
        var addedAttachment = false
        var isLinked = false

        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )
        val issue = mockIssue(
            attachments = emptyList(),
            description = PIXEL_FORMAT_CRASH,
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsDuplicate = { resolvedAsDupe = true },
            resolveAsInvalid = { resolvedAsInvalid = true },
            createLink = { type, key, _ ->
                type.shouldBe("Duplicate")
                key.shouldBe("MC-297")
                isLinked = true
            },
            addAttachment = { _, cleanupCallback ->
                addedAttachment = true
                cleanupCallback()
            }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeTrue()
        addedAttachment.shouldBeFalse()
        resolvedAsInvalid.shouldBeFalse()
        isLinked.shouldBeTrue()
    }

    "should prefer crash that is not modded, if modded crash appears first" {
        var resolvedAsDupe = false
        var resolvedAsInvalid = false
        var addedAttachment = false
        var addedComment = CommentOptions("")

        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )
        val modded = getAttachment(
            name = "crash_modded.txt",
            content = PIXEL_FORMAT_CRASH,
            created = NOW.minusMillis(10000)
        )
        val dupe = getAttachment(
            name = "crash_dupe.txt",
            content = PIXEL_FORMAT_CRASH
        )
        val issue = mockIssue(
            attachments = listOf(modded, dupe),
            description = "",
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsDuplicate = { resolvedAsDupe = true },
            resolveAsInvalid = { resolvedAsInvalid = true },
            addComment = { addedComment = it },
            addAttachment = { _, cleanupCallback ->
                addedAttachment = true
                cleanupCallback()
            }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeTrue()
        addedAttachment.shouldBeFalse()
        resolvedAsInvalid.shouldBeFalse()
        addedComment shouldBe CommentOptions("duplicate-tech", "MC-297")
    }

    "should prefer crash that is not modded, if duped crash appears first" {
        var resolvedAsDupe = false
        var resolvedAsInvalid = false
        var addedAttachment = false
        var addedComment = CommentOptions("")

        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )
        val modded = getAttachment(
            name = "crash_modded.txt",
            content = PIXEL_FORMAT_CRASH
        )
        val dupe = getAttachment(
            name = "crash_dupe.txt",
            content = PIXEL_FORMAT_CRASH,
            created = NOW.minusMillis(10000)
        )
        val issue = mockIssue(
            attachments = listOf(dupe, modded),
            description = "",
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsDuplicate = { resolvedAsDupe = true },
            resolveAsInvalid = { resolvedAsInvalid = true },
            addComment = { addedComment = it },
            addAttachment = { _, cleanupCallback ->
                addedAttachment = true
                cleanupCallback()
            }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeTrue()
        addedAttachment.shouldBeFalse()
        resolvedAsInvalid.shouldBeFalse()
        addedComment shouldBe CommentOptions("duplicate-tech", "MC-297")
    }

    "should prefer more recent crash, if less recent crash appears first " {
        var resolvedAsDupe = false
        var resolvedAsInvalid = false
        var addedAttachment = false
        var isLinked = false

        val module = CrashModule(
            listOf("txt"),
            listOf(
                CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297"),
                CrashDupeConfig("minecraft", "WGL: The driver does not appear to support OpenGL", "MC-128302")
            ),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )

        val fromNow = getAttachment(
            name = "recent.txt",
            content = PIXEL_FORMAT_CRASH
        )
        val fromYesterday = getAttachment(
            name = "crash_dupe.txt",
            content = DRIVER_NO_OPENGL,
            created = NOW.minus(1, ChronoUnit.DAYS)
        )
        val issue = mockIssue(
            attachments = listOf(fromYesterday, fromNow),
            description = "",
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsDuplicate = { resolvedAsDupe = true },
            resolveAsInvalid = { resolvedAsInvalid = true },
            createLink = { type, key, _ ->
                type.shouldBe("Duplicate")
                key.shouldBe("MC-297")
                isLinked = true
            },
            addComment = { Unit.right() },
            addAttachment = { _, cleanupCallback ->
                addedAttachment = true
                cleanupCallback()
            }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeTrue()
        addedAttachment.shouldBeFalse()
        resolvedAsInvalid.shouldBeFalse()
        isLinked.shouldBeTrue()
    }

    "should prefer more recent crash, if more recent crash appears first " {
        var resolvedAsDupe = false
        var resolvedAsInvalid = false
        var addedAttachment = false
        var isLinked = false

        val module = CrashModule(
            listOf("txt"),
            listOf(
                CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297"),
                CrashDupeConfig("minecraft", "WGL: The driver does not appear to support OpenGL", "MC-128302")
            ),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )

        val fromNow = getAttachment(
            name = "recent.txt",
            content = PIXEL_FORMAT_CRASH
        )
        val fromYesterday = getAttachment(
            name = "crash_dupe.txt",
            content = DRIVER_NO_OPENGL,
            created = NOW.minus(1, ChronoUnit.DAYS)
        )
        val issue = mockIssue(
            attachments = listOf(fromNow, fromYesterday),
            description = "",
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = NoPriority,
            resolveAsDuplicate = { resolvedAsDupe = true },
            resolveAsInvalid = { resolvedAsInvalid = true },
            createLink = { type, key, _ ->
                type.shouldBe("Duplicate")
                key.shouldBe("MC-297")
                isLinked = true
            },
            addComment = { Unit.right() },
            addAttachment = { _, cleanupCallback ->
                addedAttachment = true
                cleanupCallback()
            }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeTrue()
        addedAttachment.shouldBeFalse()
        resolvedAsInvalid.shouldBeFalse()
        isLinked.shouldBeTrue()
    }

    "should return operation not needed when the ticket is confirmed" {
        var resolvedAsDupe = false
        var resolvedAsInvalid = false

        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )
        val issue = mockIssue(
            attachments = emptyList(),
            description = PIXEL_FORMAT_CRASH,
            created = NOW,
            confirmationStatus = "Confirmed",
            priority = NoPriority,
            resolveAsDuplicate = { resolvedAsDupe = true },
            resolveAsInvalid = { resolvedAsInvalid = true },
            createLink = { _, _, _ -> Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
        resolvedAsDupe.shouldBeFalse()
        resolvedAsInvalid.shouldBeFalse()
    }

    "should return operation not needed when the ticket has priority" {
        var resolvedAsDupe = false
        var resolvedAsInvalid = false

        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            crashReader,
            "duplicate-tech",
            "modified-game"
        )
        val issue = mockIssue(
            attachments = emptyList(),
            description = PIXEL_FORMAT_CRASH,
            created = NOW,
            confirmationStatus = Unconfirmed,
            priority = "Medium",
            resolveAsDuplicate = { resolvedAsDupe = true },
            resolveAsInvalid = { resolvedAsInvalid = true },
            createLink = { _, _, _ -> Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
        resolvedAsDupe.shouldBeFalse()
        resolvedAsInvalid.shouldBeFalse()
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
