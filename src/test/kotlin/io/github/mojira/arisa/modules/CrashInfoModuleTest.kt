package io.github.mojira.arisa.modules

import arrow.core.right
import io.github.mojira.arisa.utils.mockAttachment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import com.urielsalis.mccrashlib.CrashReader
import java.time.Instant
import java.time.temporal.ChronoUnit

const val OBFUSCATED_CRASH = """---- Minecraft Crash Report ----
// Don't do that.

Time: 30/04/21 17:23
Description: mouseClicked event handler

java.lang.OutOfMemoryError: Java heap space
	at java.util.ArrayList.<init>(ArrayList.java:152)
	at com.google.common.collect.Lists.newArrayListWithCapacity(Lists.java:190)
	at mj$1.a(SourceFile:47)
	at mj$1.b(SourceFile:32)
	at md.b(SourceFile:471)
	at md.a(SourceFile:32)
	at md$1.a(SourceFile:83)
	at md$1.b(SourceFile:69)
	at md.b(SourceFile:471)
	at md.a(SourceFile:32)
	at md$1.a(SourceFile:83)
	at md$1.b(SourceFile:69)
	at md.b(SourceFile:471)
	at md.a(SourceFile:32)
	at md$1.a(SourceFile:83)
	at md$1.b(SourceFile:69)
	at mn.a(SourceFile:108)
	at mn.a(SourceFile:75)
	at mn.a(SourceFile:32)
	at mn.a(SourceFile:26)
	at cyg.a(SourceFile:229)
	at cygLambda$2987/857564250.apply(Unknown Source)
	at cyg.a(SourceFile:178)
	at cyg.b(SourceFile:157)
	at dsm.a(SourceFile:91)
	at dsm.<init>(SourceFile:83)
	at dsj.b(SourceFile:48)
	at dot.b(SourceFile:325)
	at djz.a(SourceFile:922)
	at doy.d(SourceFile:141)
	at doyLambda$2670/1502984812.onPress(Unknown Source)
	at dlj.b(SourceFile:33)

-- System Details --
Details:
	Minecraft Version: 1.16.5
	Minecraft Version ID: 1.16.5
	Operating System: Windows 10 (amd64) version 10.0
	Java Version: 1.8.0_51, Oracle Corporation
	Java VM Version: Java HotSpot(TM) 64-Bit Server VM (mixed mode), Oracle Corporation
	Memory: 546749712 bytes (521 MB) / 771751936 bytes (736 MB) up to 2147483648 bytes (2048 MB)
	CPUs: 4
	JVM Flags: 9 total; -XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump -Xss1M -Xmx2G -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M
	Launched Version: 1.16.5
	Backend library: LWJGL version 3.2.2 build 10
	Backend API: GeForce GTX 1050 Ti/PCIe/SSE2 GL version 4.6.0 NVIDIA 456.71, NVIDIA Corporation
	GL Caps: Using framebuffer using OpenGL 3.0
	Using VBOs: Yes
	Is Modded: Probably not. Jar signature remains and client brand is untouched.
	Type: Client (map_client.txt)
	Graphics mode: fast
	Resource Packs: vanilla
	Current Language: English (US)
	CPU: 4x Intel(R) Core(TM) i5-7400 CPU @ 3.00GHz
"""

const val CRASH_CODE_BLOCK = """
    {code:title=(1) [^crash.txt]}
    EXAMPLE CRASH DETAILS
    {code}
"""

private val NOW = Instant.now()
private val A_SECOND_AGO = NOW.minusSeconds(1)

private val crashReader = CrashReader()

class CrashInfoModuleTest : StringSpec({

    "should return OperationNotNeededModuleResponse when issue body does not contain any recent crash" {
        val module = CrashInfoModule(
            listOf("txt"),
            crashReader
        )

        val issue = mockIssue(
            attachments = emptyList(),
            description = PIXEL_FORMAT_CRASH,
            created = NOW.minus(42, ChronoUnit.DAYS),
            confirmationStatus = Unconfirmed,
            resolveAsInvalid = { Unit.right() },
            resolveAsDuplicate = { Unit.right() },
            createLink = { _, _, _ -> Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when issue does not contain any recent crash as attachment" {
        val module = CrashInfoModule(
            listOf("txt"),
            crashReader
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
            resolveAsInvalid = { Unit.right() },
            resolveAsDuplicate = { Unit.right() },
            createLink = { _, _, _ -> Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)
        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when attached crash does have a wrong mime type" {
        val module = CrashInfoModule(
            listOf("txt"),
            crashReader
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
            resolveAsInvalid = { Unit.right() },
            resolveAsDuplicate = { Unit.right() },
            createLink = { _, _, _ -> Unit.right() },
            addComment = { Unit.right() }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should add attachment when deobfuscated" {
        var resolvedAsDupe = false
        var resolvedAsInvalid = false
        var addedAttachment = false

        val module = CrashInfoModule(
            listOf("txt"),
            crashReader
        )
        val issue = mockIssue(
            attachments = listOf(getAttachment(OBFUSCATED_CRASH)),
            description = OBFUSCATED_CRASH,
            created = NOW,
            confirmationStatus = Unconfirmed,
            resolveAsDuplicate = { resolvedAsDupe = true },
            resolveAsInvalid = { resolvedAsInvalid = true },
            addAttachment = { _, _ -> addedAttachment = true }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeFalse()
        addedAttachment.shouldBeTrue()
        resolvedAsInvalid.shouldBeFalse()
    }

    "should add to description after deobfuscated and none exists" {
        var resolvedAsDupe = false
        var resolvedAsInvalid = false
        var addedAttachment = false
        var addedCrash = false

        val module = CrashInfoModule(
            listOf("txt"),
            crashReader
        )
        val issue = mockIssue(
            attachments = listOf(getAttachment(OBFUSCATED_CRASH)),
            description = OBFUSCATED_CRASH,
            created = NOW,
            confirmationStatus = Unconfirmed,
            resolveAsDuplicate = { resolvedAsDupe = true },
            resolveAsInvalid = { resolvedAsInvalid = true },
            addAttachment = { _, _ -> addedAttachment = true },
            updateDescription = { addedCrash = true }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeFalse()
        addedAttachment.shouldBeTrue()
        resolvedAsInvalid.shouldBeFalse()
        addedCrash.shouldBeTrue()
    }

    "should not add to description after deobfuscated and crash already exists" {
        var resolvedAsDupe = false
        var resolvedAsInvalid = false
        var addedAttachment = false
        var addedCrash = false

        val module = CrashInfoModule(
            listOf("txt"),
            crashReader
        )
        val issue = mockIssue(
            attachments = listOf(getAttachment(OBFUSCATED_CRASH)),
            description = OBFUSCATED_CRASH + "\n" + CRASH_CODE_BLOCK,
            created = NOW,
            confirmationStatus = Unconfirmed,
            resolveAsDuplicate = { resolvedAsDupe = true },
            resolveAsInvalid = { resolvedAsInvalid = true },
            addAttachment = { _, _ -> addedAttachment = true },
            updateDescription = { addedCrash = true }
        )

        val result = module(issue, A_SECOND_AGO)

        result.shouldBeRight(ModuleResponse)
        resolvedAsDupe.shouldBeFalse()
        addedAttachment.shouldBeTrue()
        resolvedAsInvalid.shouldBeFalse()
        addedCrash.shouldBeFalse()
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
