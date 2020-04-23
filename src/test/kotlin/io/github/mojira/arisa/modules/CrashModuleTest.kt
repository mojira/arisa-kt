package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.infrastructure.config.CrashDupeConfig
import io.github.mojira.arisa.modules.CrashModule.Attachment
import io.github.mojira.arisa.modules.CrashModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import me.urielsalis.mccrashlib.CrashReader
import java.util.Calendar
import java.util.Calendar.DAY_OF_YEAR
import java.util.Date

const val EXAMPLE_CRASH = """---- Minecraft Crash Report ----
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

const val SERVER_UNMODDED_CRASH = """---- Minecraft Crash Report ----
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

const val SERVER_MODDED_CRASH = """---- Minecraft Crash Report ----
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

const val SERVER_MODDED_CRASH_2 = """---- Minecraft Crash Report ----
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

const val EXAMPLE_CRASH_2 = """---- Minecraft Crash Report ----
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

const val MODDED_CRASH = """---- Minecraft Crash Report ----
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

const val Unconfirmed = "Unconfirmed"
val NoPriority = null

val crashReader = CrashReader()

class CrashModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when issue does not contain any valid crash report" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            10,
            crashReader
        )
        val request = Request(
            emptyList(),
            "Help\nmy\ngame\nis\nsuper\nbroken!!\n!!!",
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when issue body does not contain any recent crash" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            10,
            crashReader
        )

        val calendar = Calendar.getInstance()
        calendar.add(DAY_OF_YEAR, -42)
        val time = calendar.time

        val request = Request(
            emptyList(),
            EXAMPLE_CRASH,
            time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when crash configurations are empty and report is not modded" {
        val module = CrashModule(
            listOf("txt"),
            emptyList(),
            10,
            crashReader
        )
        val request = Request(
            emptyList(),
            EXAMPLE_CRASH,
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when crash configuration has an invalid type and crash is not modded" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("hytale", "The game has not yet been released", "HT-1")),
            10,
            crashReader
        )

        val request = Request(
            emptyList(),
            EXAMPLE_CRASH,
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when reported crash is not configured and not modded" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Unexpected loophole in Redstone implementation", "MC-108")),
            10,
            crashReader
        )

        val request = Request(
            emptyList(),
            EXAMPLE_CRASH,
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when issue does not contain any recent crash as attachment" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            10,
            crashReader
        )

        val calendar = Calendar.getInstance()
        calendar.add(DAY_OF_YEAR, -42)
        val time = calendar.time
        val attachment = Attachment("crash.txt", time) { EXAMPLE_CRASH.toByteArray() }

        val request = Request(
            listOf(attachment),
            "",
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)
        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when attached crash is not configured and not modded" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Unexpected loophole in Redstone implementation", "MC-108")),
            10,
            crashReader
        )

        val attachment = Attachment("crash.txt", Calendar.getInstance().time) { EXAMPLE_CRASH.toByteArray() }
        val request = Request(
            listOf(attachment),
            "",
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when attached crash does have a wrong mime type" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            10,
            crashReader
        )

        val attachment = Attachment("crash.png", Calendar.getInstance().time) { EXAMPLE_CRASH.toByteArray() }
        val request = Request(
            listOf(attachment),
            "",
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when attached crash is not modded (Unknown)" {
        val module = CrashModule(
            listOf("txt"),
            listOf(),
            10,
            crashReader
        )

        val attachment = Attachment("crash.txt", Calendar.getInstance().time) { SERVER_UNMODDED_CRASH.toByteArray() }
        val request = Request(
            listOf(attachment),
            "",
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid when reported server crash is modded" {
        var resolvedAsInvalid = false

        val module = CrashModule(
            listOf("txt"),
            emptyList(),
            10,
            crashReader
        )
        val request = Request(
            emptyList(),
            SERVER_MODDED_CRASH,
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { resolvedAsInvalid = true; Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        resolvedAsInvalid.shouldBeTrue()
    }

    "should resolve as invalid when reported server crash is very likely modded" {
        var resolvedAsInvalid = false

        val module = CrashModule(
            listOf("txt"),
            emptyList(),
            10,
            crashReader
        )
        val request = Request(
            emptyList(),
            SERVER_MODDED_CRASH_2,
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { resolvedAsInvalid = true; Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        resolvedAsInvalid.shouldBeTrue()
    }

    "should resolve as invalid when reported crash is modded" {
        var resolvedAsInvalid = false

        val module = CrashModule(
            listOf("txt"),
            emptyList(),
            10,
            crashReader
        )
        val request = Request(
            emptyList(),
            MODDED_CRASH,
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { resolvedAsInvalid = true; Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        resolvedAsInvalid.shouldBeTrue()
    }

    "should resolve as invalid when attached crash is modded" {
        var resolvedAsInvalid = false

        val module = CrashModule(
            listOf("txt"),
            emptyList(),
            10,
            crashReader
        )

        val attachment = Attachment("crash.txt", Calendar.getInstance().time) { MODDED_CRASH.toByteArray() }
        val request = Request(
            listOf(attachment),
            "",
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { resolvedAsInvalid = true; Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        resolvedAsInvalid.shouldBeTrue()
    }

    "should resolve as dupe when reported crash is in configured list" {
        var resolvedAsDupe = false

        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            10,
            crashReader
        )
        val request = Request(
            emptyList(),
            EXAMPLE_CRASH,
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { resolvedAsDupe = true; Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeTrue()
    }

    "should resolve as dupe when attached crash is in configured list" {
        var resolvedAsDupe = false

        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            10,
            crashReader
        )
        val attachment = Attachment("crash.txt", Calendar.getInstance().time) { EXAMPLE_CRASH.toByteArray() }
        val request = Request(
            listOf(attachment),
            "",
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { resolvedAsDupe = true; Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeTrue()
    }

    "should resolve as dupe when the configured crash is a java crash" {
        var resolvedAsDupe = false

        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("java", "ig75icd64\\.dll", "MC-32606")),
            10,
            crashReader
        )
        val request = Request(
            emptyList(),
            JAVA_CRASH,
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { resolvedAsDupe = true; Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeTrue()
    }

    "should resolve as dupe when the configured crash uses regex" {
        var resolvedAsDupe = false

        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("java", "ig[0-9]{1,2}icd[0-9]{2}\\.dll", "MC-32606")),
            10,
            crashReader
        )
        val request = Request(
            emptyList(),
            JAVA_CRASH,
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { resolvedAsDupe = true; Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeTrue()
    }

    "should link to configured ticket when resolving as dupe" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            10,
            crashReader
        )
        val request = Request(
            emptyList(),
            EXAMPLE_CRASH,
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { Unit.right() },
            { it.shouldBe("MC-297").right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should prefer crash that is not modded, if modded crash appears first" {
        var resolvedAsDupe = false

        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            10,
            crashReader
        )
        val modded = Attachment(
            "crash_modded.txt",
            Date.from(Calendar.getInstance().time.toInstant().minusMillis(10000))
        ) { EXAMPLE_CRASH.toByteArray() }
        val dupe = Attachment("crash_dupe.txt", Calendar.getInstance().time) { EXAMPLE_CRASH.toByteArray() }
        val request = Request(
            listOf(modded, dupe),
            "",
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { resolvedAsDupe = true; Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeTrue()
    }

    "should prefer crash that is not modded, if duped crash appears first" {
        var resolvedAsDupe = false

        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            10,
            crashReader
        )
        val modded = Attachment("crash_modded.txt", Calendar.getInstance().time) { EXAMPLE_CRASH.toByteArray() }
        val dupe = Attachment(
            "crash_dupe.txt",
            Date.from(Calendar.getInstance().time.toInstant().minusMillis(10000))
        ) { EXAMPLE_CRASH.toByteArray() }
        val request = Request(
            listOf(dupe, modded),
            "",
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { resolvedAsDupe = true; Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeTrue()
    }

    "should prefer more recent crash, if less recent crash appears first " {
        val module = CrashModule(
            listOf("txt"),
            listOf(
                CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297"),
                CrashDupeConfig("minecraft", "WGL: The driver does not appear to support OpenGL", "MC-128302")
            ),
            10,
            crashReader
        )
        val calendar = Calendar.getInstance()
        calendar.add(DAY_OF_YEAR, -1)
        val yesterday = calendar.time

        val fromNow = Attachment("recent.txt", Calendar.getInstance().time) { EXAMPLE_CRASH.toByteArray() }
        val fromYesterday = Attachment("crash_dupe.txt", yesterday) { EXAMPLE_CRASH_2.toByteArray() }
        val request = Request(
            listOf(fromYesterday, fromNow),
            "",
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { Unit.right() },
            { it.shouldBe("MC-297").right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should prefer more recent crash, if more recent crash appears first " {
        val module = CrashModule(
            listOf("txt"),
            listOf(
                CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297"),
                CrashDupeConfig("minecraft", "WGL: The driver does not appear to support OpenGL", "MC-128302")
            ),
            10,
            crashReader
        )
        val calendar = Calendar.getInstance()
        calendar.add(DAY_OF_YEAR, -1)
        val yesterday = calendar.time

        val fromNow = Attachment("recent.txt", Calendar.getInstance().time) { EXAMPLE_CRASH.toByteArray() }
        val fromYesterday = Attachment("crash_dupe.txt", yesterday) { EXAMPLE_CRASH_2.toByteArray() }
        val request = Request(
            listOf(fromNow, fromYesterday),
            "",
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { Unit.right() },
            { it.shouldBe("MC-297").right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when resolving as invalid fails" {
        val module = CrashModule(
            listOf("txt"),
            emptyList(),
            10,
            crashReader
        )
        val request = Request(
            emptyList(),
            MODDED_CRASH,
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { RuntimeException().left() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when sending as modded message fails" {
        val module = CrashModule(
            listOf("txt"),
            emptyList(),
            10,
            crashReader
        )
        val request = Request(
            emptyList(),
            MODDED_CRASH,
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { RuntimeException().left() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when resolving as duplicate fails" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            10,
            crashReader
        )
        val request = Request(
            emptyList(),
            EXAMPLE_CRASH,
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { RuntimeException().left() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when linking a duplicate fails" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            10,
            crashReader
        )
        val request = Request(
            emptyList(),
            EXAMPLE_CRASH,
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { Unit.right() },
            { RuntimeException().left() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when sending duplicate message fails" {
        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            10,
            crashReader
        )
        val request = Request(
            emptyList(),
            EXAMPLE_CRASH,
            Calendar.getInstance().time,
            Unconfirmed,
            NoPriority,
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { RuntimeException().left() }
        )

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return operation not needed when the ticket is confirmed" {
        var resolvedAsDupe = false

        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            10,
            crashReader
        )
        val request = Request(
            emptyList(),
            EXAMPLE_CRASH,
            Calendar.getInstance().time,
            "Confirmed",
            NoPriority,
            { Unit.right() },
            { resolvedAsDupe = true; Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
        resolvedAsDupe.shouldBeFalse()
    }

    "should return operation not needed when the ticket has priority" {
        var resolvedAsDupe = false

        val module = CrashModule(
            listOf("txt"),
            listOf(CrashDupeConfig("minecraft", "Pixel format not accelerated", "MC-297")),
            10,
            crashReader
        )
        val request = Request(
            emptyList(),
            EXAMPLE_CRASH,
            Calendar.getInstance().time,
            Unconfirmed,
            "Medium",
            { Unit.right() },
            { resolvedAsDupe = true; Unit.right() },
            { Unit.right() },
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
        resolvedAsDupe.shouldBeFalse()
    }
})
