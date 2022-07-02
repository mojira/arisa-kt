package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.firstOrNone
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially2
import com.urielsalis.mccrashlib.Crash
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.AttachmentUtils
import io.github.mojira.arisa.infrastructure.config.JvmCrashDupeConfig
import io.github.mojira.arisa.infrastructure.config.MinecraftCrashDupeConfig
import io.github.mojira.arisa.infrastructure.getDeobfName
import java.time.Instant

@Suppress("LongParameterList")
class CrashModule(
    private val attachmentUtils: AttachmentUtils,
    private val minecraftCrashDupeConfigs: List<MinecraftCrashDupeConfig>,
    private val jvmCrashDupeConfigs: List<JvmCrashDupeConfig>,
    private val dupeMessage: String,
    private val moddedMessage: String
) : Module {

    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            // Extract crashes from attachments
            val crashes = attachmentUtils.extractCrashesFromAttachments(issue)

            // Only check crashes added since the last run
            val newCrashes = getNewCrashes(crashes, lastRun).bind()

            // Deobfuscate crashes, if necessary
            uploadDeobfuscatedCrashes(issue, newCrashes)

            // Check if the bug report should be closed because no valid crashes are attached
            assertNoValidCrash(crashes).bind()

            // Only close bug reports that are unconfirmed and untriaged
            assertEquals(confirmationStatus ?: "Unconfirmed", "Unconfirmed").bind()
            assertNull(priority).bind()

            // Get parent bug report key
            val parentKey = crashes
                .sortedByDescending { it.document.created } // newest crashes first
                .firstNotNullOfOrNull { getDuplicateLink(it.crash) }

            if (parentKey == null) {
                resolveAsInvalid()
                addComment(CommentOptions(moddedMessage))
            } else {
                resolveAsDuplicate()
                addComment(CommentOptions(dupeMessage, parentKey))
                createLink("Duplicate", parentKey, true)
            }
        }
    }

    private fun uploadDeobfuscatedCrashes(issue: Issue, crashAttachments: List<AttachmentUtils.CrashAttachment>) {
        val minecraftCrashesWithDeobf = crashAttachments
            .asSequence()
            .mapNotNull(::deobfuscate)
            .filterNot {
                // Don't upload new deobfuscated crash file if it already exists
                issue.attachments.any { attachment -> attachment.name == it.name }
            }
            .toList()

        minecraftCrashesWithDeobf.forEach {
            issue.addAttachment(it.name, it.deobfCrashReport)
        }
    }

    private data class DeobfuscatedCrashAttachment(
        val name: String,
        val deobfCrashReport: String
    )

    // Deobfuscate the crash in the given crash attachment.
    // If crash cannot be deobfuscated, returns null.
    // Otherwise returns data about the deobfuscated crash attachment to be uploaded.
    private fun deobfuscate(attachment: AttachmentUtils.CrashAttachment): DeobfuscatedCrashAttachment? {
        return if (attachment.crash is Crash.Minecraft) {
            val name = getDeobfName(attachment.document.name)
            val deobfCrashReport = attachment.crash.deobf ?: return null

            DeobfuscatedCrashAttachment(name, deobfCrashReport)
        } else {
            null
        }
    }

    /**
     * Checks whether an analyzed crash report matches any of the specified known crash issues.
     * Returns the key of the parent bug report if one is found, and null otherwise.
     */
    private fun getDuplicateLink(crash: Crash): String? {
        return when (crash) {
            is Crash.Minecraft -> minecraftCrashDupeConfigs
                .firstOrNone { it.exceptionRegex.toRegex().containsMatchIn(crash.exception) }
                .orNull()
                ?.duplicates
            is Crash.Jvm -> (crash.problematicFrame as? Crash.JvmFrame.CFrame)?.libraryName?.let { libraryName ->
                jvmCrashDupeConfigs
                    .firstOrNone { it.libraryNameRegex.toRegex().containsMatchIn(libraryName) }
                    .orNull()
                    ?.duplicates
            }
            else -> null
        }
    }

    private fun isModded(crash: Crash) =
        (crash is Crash.Minecraft && crash.modded) || (crash is Crash.Jvm && crash.isModded)

    private fun crashNewlyAdded(attachment: AttachmentUtils.CrashAttachment, lastRun: Instant) =
        attachment.document.created.isAfter(lastRun)

    private fun getNewCrashes(
        crashes: List<AttachmentUtils.CrashAttachment>,
        lastRun: Instant
    ): Either<OperationNotNeededModuleResponse, List<AttachmentUtils.CrashAttachment>> {
        val newCrashes = crashes.filter(::crashNewlyAdded.partially2(lastRun))
        return if (newCrashes.isNotEmpty()) {
            newCrashes.right()
        } else {
            OperationNotNeededModuleResponse.left()
        }
    }

    private fun assertNoValidCrash(crashes: List<AttachmentUtils.CrashAttachment>) =
        if (crashes.all { isModded(it.crash) || getDuplicateLink(it.crash) != null })
            Unit.right()
        else
            OperationNotNeededModuleResponse.left()
}
