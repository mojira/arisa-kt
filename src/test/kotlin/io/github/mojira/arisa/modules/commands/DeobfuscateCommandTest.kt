package io.github.mojira.arisa.modules.commands

import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.urielsalis.mccrashlib.CrashReader
import io.github.mojira.arisa.infrastructure.AttachmentUtils
import io.github.mojira.arisa.utils.mockAttachment
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private fun String.normalizeLineTerminators() = trim().replace(Regex("\\R"), "\n")

private val SERVER_CRASH_WITH_VERSION = """
---- Minecraft Crash Report ----
// Shall we play a game?

Time: 10/3/21, 10:11 AM
Description: Watching Server

java.lang.Error: Watchdog
    at abg.run(SourceFile:65)
    at java.base/java.lang.Thread.run(Thread.java:831)

A detailed walkthrough of the error, its code path and all known details is as follows:
---------------------------------------------------------------------------------------

-- Head --
Thread: Server Watchdog
Stacktrace:
    at abg.run(SourceFile:65)
    at java.base/java.lang.Thread.run(Thread.java:831)

-- System Details --
Details:
    Minecraft Version: 21w39a
    Minecraft Version ID: 21w39a
    Player Count: 0 / 12
    Data Packs: vanilla
    Is Modded: Unknown (can't tell)
    Type: Dedicated Server (map_server.txt)
""".trim()

/** Has bad `Minecraft Version ID` value */
private val CRASH_WITH_BAD_VERSION = """
---- Minecraft Crash Report ----
// Why is it breaking :(

Time: 02/10/2021 09:31
Description: Unexpected error

java.lang.NullPointerException: Cannot invoke "dsb.d()" because "something" is null
    at eox.a(SourceFile:1678)
    at eox.a(SourceFile:1258)
    at eos.a(SourceFile:1026)
    at eos.a(SourceFile:810)
    at dxd.f(SourceFile:1119)
    at dxd.e(SourceFile:735)
    at net.minecraft.client.main.Main.main(SourceFile:238)


A detailed walkthrough of the error, its code path and all known details is as follows:
---------------------------------------------------------------------------------------

-- Head --
Thread: Render thread
Stacktrace:
    at eox.a(SourceFile:1678)
    at eox.a(SourceFile:1258)
    at eos.a(SourceFile:1026)

-- Affected level --
Details:
    All players: 0 total
    Chunk stats: 961, 405
    Level dimension: minecraft:overworld
    Level spawn location: World: (-96,75,80), Section: (at 0,11,0 in -6,4,5; chunk contains blocks -96,-64,80 to -81,319,95), Region: (-1,0; contains chunks -32,0 to -1,31, blocks -512,-64,0 to -1,319,511)
    Level time: 15454 game time, 15454 day time
    Server brand: vanilla
    Server type: Integrated singleplayer server
Stacktrace:
    at ekz.a(SourceFile:396)
    at dxd.c(SourceFile:2411)
    at dxd.e(SourceFile:759)
    at net.minecraft.client.main.Main.main(SourceFile:238)

-- Last reload --
Details:
    Reload number: 1
    Reload reason: initial
    Finished: Yes
    Packs: Default

-- System Details --
Details:
    Minecraft Version: 21w39a
    Minecraft Version ID: Minecraft.Server / f7d695aa1ba843f2aa0cbc2ece6aea49
    Operating System: Linux (amd64)
    Java Version: 17, BellSoft
    Launched Version: 21w39a
    Is Modded: Probably not. Jar signature remains and both client + server brands are untouched.
    Type: Integrated Server (map_client.txt)
    Player Count: 0 / 8
    Data Packs: vanilla
""".trim()

private val SERVER_CRASH_WITHOUT_VERSION_AND_TYPE = """
---- Minecraft Crash Report ----
// Shall we play a game?

Time: 10/3/21, 10:11 AM
Description: Watching Server

java.lang.Error: Watchdog
    at abg.run(SourceFile:65)
    at java.base/java.lang.Thread.run(Thread.java:831)

A detailed walkthrough of the error, its code path and all known details is as follows:
---------------------------------------------------------------------------------------

-- Head --
Thread: Server Watchdog
Stacktrace:
    at abg.run(SourceFile:65)
    at java.base/java.lang.Thread.run(Thread.java:831)

-- System Details --
Details:
    Operating System: Linux (amd64)
    Java Version: 16.0.2, Oracle Corporation
    Player Count: 0 / 12
    Data Packs: vanilla
    Is Modded: Unknown (can't tell)
""".trim()

class DeobfuscateCommandTest : StringSpec({
    val command = DeobfuscateCommand(AttachmentUtils(emptyList(), CrashReader(), "bot"))

    "should throw for unknown attachment ID" {
        val issue = mockIssue()
        val e = shouldThrow<CommandSyntaxException> { command.invoke(issue, "does-not-exist") }
        e.message shouldBe "Attachment with ID 'does-not-exist' does not exist"
    }

    "should throw for conflicting attachment name" {
        val attachmentId = "123"
        val issue = mockIssue(
            attachments = listOf(
                mockAttachment(
                    id = attachmentId,
                    name = "test.txt"
                ),
                mockAttachment(
                    name = "deobf_test.txt"
                )
            )
        )

        val e = shouldThrow<CommandSyntaxException> { command.invoke(issue, attachmentId) }
        e.message shouldBe "Attachment with name 'deobf_test.txt' already exists"
    }

    "should throw for missing version when not present in crash" {
        val attachmentId = "123"
        val issue = mockIssue(
            attachments = listOf(
                mockAttachment(
                    id = attachmentId,
                    getContent = { SERVER_CRASH_WITHOUT_VERSION_AND_TYPE.toByteArray() }
                )
            )
        )

        val e = shouldThrow<CommandSyntaxException> { command.invoke(issue, attachmentId) }
        e.message shouldBe "Version (and crash report type) could not be detected; must be specified manually"
    }

    "should throw for unknown provided version" {
        var addedAttachment = false

        val attachmentId = "123"
        val issue = mockIssue(
            attachments = listOf(
                mockAttachment(
                    id = attachmentId,
                    name = "crash.txt",
                    getContent = { SERVER_CRASH_WITH_VERSION.toByteArray() }
                )
            ),
            addAttachment = { _, _ -> addedAttachment = true }
        )

        val e = shouldThrow<CommandExceptions.CommandExecutionException> {
            command.invoke(issue, attachmentId, "unknown-version")
        }
        e.message shouldBe "Deobfuscation of attachment with ID '123' failed: Unknown version 'unknown-version'"

        addedAttachment shouldBe false
    }

    "should deobfuscate when version and mapping type are present in crash and not provided in command" {
        var addedAttachmentName: String? = null
        var addedAttachmentContent: String? = null

        val attachmentId = "123"
        val issue = mockIssue(
            attachments = listOf(
                mockAttachment(
                    id = attachmentId,
                    name = "crash.txt",
                    getContent = { SERVER_CRASH_WITH_VERSION.toByteArray() }
                )
            ),
            addAttachment = { name, content ->
                addedAttachmentName = name
                addedAttachmentContent = content
            }
        )

        command.invoke(issue, attachmentId)
        addedAttachmentName shouldBe "deobf_crash.txt"
        addedAttachmentContent?.normalizeLineTerminators() shouldBe """
            ---- Minecraft Crash Report ----
            // Shall we play a game?
            
            Time: 10/3/21, 10:11 AM
            Description: Watching Server
            
            java.lang.Error: Watchdog
                at net.minecraft.server.dedicated.ServerWatchdog.void run()(ServerWatchdog.java:65)
                at java.base/java.lang.Thread.run(Thread.java:831)
            
            A detailed walkthrough of the error, its code path and all known details is as follows:
            ---------------------------------------------------------------------------------------
            
            -- Head --
            Thread: Server Watchdog
            Stacktrace:
                at net.minecraft.server.dedicated.ServerWatchdog.void run()(ServerWatchdog.java:65)
                at java.base/java.lang.Thread.run(Thread.java:831)
            
            -- System Details --
            Details:
                Minecraft Version: 21w39a
                Minecraft Version ID: 21w39a
                Player Count: 0 / 12
                Data Packs: vanilla
                Is Modded: Unknown (can't tell)
                Type: Dedicated Server (map_server.txt)
        """.trimIndent()
    }

    "should allow overwriting bad version in crash report" {
        var addedAttachmentName: String? = null
        var addedAttachmentContent: String? = null

        val attachmentId = "123"
        val issue = mockIssue(
            attachments = listOf(
                mockAttachment(
                    id = attachmentId,
                    name = "crash.txt",
                    getContent = { CRASH_WITH_BAD_VERSION.toByteArray() }
                )
            ),
            addAttachment = { name, content ->
                addedAttachmentName = name
                addedAttachmentContent = content
            }
        )

        command.invoke(issue, attachmentId, "21w39a", CrashReportType.CLIENT)
        addedAttachmentName shouldBe "deobf_crash.txt"
        addedAttachmentContent?.normalizeLineTerminators() shouldBe """
            ---- Minecraft Crash Report ----
            // Why is it breaking :(
            
            Time: 02/10/2021 09:31
            Description: Unexpected error
            
            java.lang.NullPointerException: Cannot invoke "dsb.d()" because "something" is null
                at net.minecraft.client.renderer.LevelRenderer.void renderChunkLayer(net.minecraft.client.renderer.RenderType,com.mojang.blaze3d.vertex.PoseStack,double,double,double,com.mojang.math.Matrix4f)(LevelRenderer.java:1678)
                at net.minecraft.client.renderer.LevelRenderer.void renderLevel(com.mojang.blaze3d.vertex.PoseStack,float,long,boolean,net.minecraft.client.Camera,net.minecraft.client.renderer.GameRenderer,net.minecraft.client.renderer.LightTexture,com.mojang.math.Matrix4f)(LevelRenderer.java:1258)
                at net.minecraft.client.renderer.GameRenderer.void renderLevel(float,long,com.mojang.blaze3d.vertex.PoseStack)(GameRenderer.java:1026)
                at net.minecraft.client.renderer.GameRenderer.void render(float,long,boolean)(GameRenderer.java:810)
                at net.minecraft.client.Minecraft.void runTick(boolean)(Minecraft.java:1119)
                at net.minecraft.client.Minecraft.void run()(Minecraft.java:735)
                at net.minecraft.client.main.Main.void main(java.lang.String[])(Main.java:238)
            
            
            A detailed walkthrough of the error, its code path and all known details is as follows:
            ---------------------------------------------------------------------------------------
            
            -- Head --
            Thread: Render thread
            Stacktrace:
                at net.minecraft.client.renderer.LevelRenderer.void renderChunkLayer(net.minecraft.client.renderer.RenderType,com.mojang.blaze3d.vertex.PoseStack,double,double,double,com.mojang.math.Matrix4f)(LevelRenderer.java:1678)
                at net.minecraft.client.renderer.LevelRenderer.void renderLevel(com.mojang.blaze3d.vertex.PoseStack,float,long,boolean,net.minecraft.client.Camera,net.minecraft.client.renderer.GameRenderer,net.minecraft.client.renderer.LightTexture,com.mojang.math.Matrix4f)(LevelRenderer.java:1258)
                at net.minecraft.client.renderer.GameRenderer.void renderLevel(float,long,com.mojang.blaze3d.vertex.PoseStack)(GameRenderer.java:1026)
            
            -- Affected level --
            Details:
                All players: 0 total
                Chunk stats: 961, 405
                Level dimension: minecraft:overworld
                Level spawn location: World: (-96,75,80), Section: (at 0,11,0 in -6,4,5; chunk contains blocks -96,-64,80 to -81,319,95), Region: (-1,0; contains chunks -32,0 to -1,31, blocks -512,-64,0 to -1,319,511)
                Level time: 15454 game time, 15454 day time
                Server brand: vanilla
                Server type: Integrated singleplayer server
            Stacktrace:
                at net.minecraft.client.multiplayer.ClientLevel.net.minecraft.CrashReportCategory fillReportDetails(net.minecraft.CrashReport)(ClientLevel.java:396)
                at net.minecraft.client.Minecraft.net.minecraft.CrashReport fillReport(net.minecraft.CrashReport)(Minecraft.java:2411)
                at net.minecraft.client.Minecraft.void run()(Minecraft.java:759)
                at net.minecraft.client.main.Main.void main(java.lang.String[])(Main.java:238)
            
            -- Last reload --
            Details:
                Reload number: 1
                Reload reason: initial
                Finished: Yes
                Packs: Default
            
            -- System Details --
            Details:
                Minecraft Version: 21w39a
                Minecraft Version ID: Minecraft.Server / f7d695aa1ba843f2aa0cbc2ece6aea49
                Operating System: Linux (amd64)
                Java Version: 17, BellSoft
                Launched Version: 21w39a
                Is Modded: Probably not. Jar signature remains and both client + server brands are untouched.
                Type: Integrated Server (map_client.txt)
                Player Count: 0 / 8
                Data Packs: vanilla
        """.trimIndent()
    }

    "should allow specifying version and type when both are missing" {
        var addedAttachmentName: String? = null
        var addedAttachmentContent: String? = null

        val attachmentId = "123"
        val issue = mockIssue(
            attachments = listOf(
                mockAttachment(
                    id = attachmentId,
                    name = "crash.txt",
                    getContent = { SERVER_CRASH_WITHOUT_VERSION_AND_TYPE.toByteArray() }
                )
            ),
            addAttachment = { name, content ->
                addedAttachmentName = name
                addedAttachmentContent = content
            }
        )

        // Crash is server crash, but deobfuscate as client crash here
        command.invoke(issue, attachmentId, "21w39a", CrashReportType.CLIENT)
        addedAttachmentName shouldBe "deobf_crash.txt"
        addedAttachmentContent?.normalizeLineTerminators() shouldBe """
            ---- Minecraft Crash Report ----
            // Shall we play a game?
            
            Time: 10/3/21, 10:11 AM
            Description: Watching Server
            
            java.lang.Error: Watchdog
                at net.minecraft.server.dedicated.ServerWatchdog.void run()(ServerWatchdog.java:65)
                at java.base/java.lang.Thread.run(Thread.java:831)
            
            A detailed walkthrough of the error, its code path and all known details is as follows:
            ---------------------------------------------------------------------------------------
            
            -- Head --
            Thread: Server Watchdog
            Stacktrace:
                at net.minecraft.server.dedicated.ServerWatchdog.void run()(ServerWatchdog.java:65)
                at java.base/java.lang.Thread.run(Thread.java:831)
            
            -- System Details --
            Details:
                Operating System: Linux (amd64)
                Java Version: 16.0.2, Oracle Corporation
                Player Count: 0 / 12
                Data Packs: vanilla
                Is Modded: Unknown (can't tell)
        """.trimIndent()
    }
})
