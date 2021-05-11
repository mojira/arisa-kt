package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.firstOrNone
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially2
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.AttachmentUtils
import io.github.mojira.arisa.infrastructure.config.CrashDupeConfig
import me.urielsalis.mccrashlib.Crash
import me.urielsalis.mccrashlib.CrashReader
import java.io.File
import java.time.Instant

class CrashModule(
    private val crashReportExtensions: List<String>,
    private val crashDupeConfigs: List<CrashDupeConfig>,
    private val crashReader: CrashReader,
    private val dupeMessage: String,
    private val moddedMessage: String
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertEquals(confirmationStatus ?: "Unconfirmed", "Unconfirmed").bind()
            assertNull(priority).bind()

            val crashes = AttachmentUtils(crashReportExtensions, crashReader).extractCrashesFromAttachments(issue)

            val newCrashes = assertContainsNewCrash(crashes, lastRun).bind()
            uploadDeobfuscatedCrashes(issue, newCrashes)
            assertNoValidCrash(crashes).bind()

            val key = crashes
                .sortedByDescending { it.first.created }
                .mapNotNull(::getDuplicateLink.partially2(crashDupeConfigs))
                .firstOrNull()

            if (key == null) {
                resolveAsInvalid()
                addComment(CommentOptions(moddedMessage))
            } else {
                resolveAsDuplicate()
                addComment(CommentOptions(dupeMessage, key))
                createLink("Duplicate", key, true)
            }
        }
    }

    private fun uploadDeobfuscatedCrashes(issue: Issue, crashes: List<Pair<AttachmentUtils.TextDocument, Crash>>) {
        val minecraftCrashesWithDeobf = crashes
            .map { it.first.name to it.second }
            .filter { it.second is Crash.Minecraft }
            .map { it.first to (it.second as Crash.Minecraft).deobf }
            .filter { it.second != null }
            .filterNot { issue.attachments.any { attachment -> attachment.name == getDeobfName(it.first) } }
        minecraftCrashesWithDeobf.forEach {
            issue.addAttachment(File(getDeobfName(it.first)))
        }
    }

    private fun getDeobfName(name: String): String = "${name.substringBeforeLast(".")}-deobfuscated.txt"

    /**
     * Checks whether an analyzed crash report matches any of the specified known crash issues.
     * Returns the key of the parent bug report if one is found, and null otherwise.
     */
    private fun getDuplicateLink(
        crash: Pair<AttachmentUtils.TextDocument, Crash>,
        crashDupeConfigs: List<CrashDupeConfig>
    ): String? = with(crash.second) {
        val minecraftConfigs = crashDupeConfigs.filter { it.type == "minecraft" }
        val javaConfigs = crashDupeConfigs.filter { it.type == "java" }

        return when (this) {
            is Crash.Minecraft -> minecraftConfigs
                .firstOrNone { it.exceptionRegex.toRegex().containsMatchIn(exception) }
                .orNull()
                ?.duplicates
            is Crash.Java -> javaConfigs
                .firstOrNone { it.exceptionRegex.toRegex().containsMatchIn(code) }
                .orNull()
                ?.duplicates
            else -> null
        }
    }

    private fun isModded(crash: Pair<AttachmentUtils.TextDocument, Crash>) =
        crash.second is Crash.Minecraft && (crash.second as Crash.Minecraft).modded

    private fun crashNewlyAdded(crash: Pair<AttachmentUtils.TextDocument, Crash>, lastRun: Instant) =
        crash.first.created.isAfter(lastRun)

    private fun assertContainsNewCrash(
        crashes: List<Pair<AttachmentUtils.TextDocument, Crash>>,
        lastRun: Instant
    ): Either<OperationNotNeededModuleResponse, List<Pair<AttachmentUtils.TextDocument, Crash>>> {
        val newCrashes = crashes.filter(::crashNewlyAdded.partially2(lastRun))
        return if (newCrashes.isNotEmpty()) {
            newCrashes.right()
        } else {
            OperationNotNeededModuleResponse.left()
        }
    }

    private fun assertNoValidCrash(crashes: List<Pair<AttachmentUtils.TextDocument, Crash>>) =
        if (crashes.all { isModded(it) || getDuplicateLink(it, crashDupeConfigs) != null })
            Unit.right()
        else
            OperationNotNeededModuleResponse.left()
}
