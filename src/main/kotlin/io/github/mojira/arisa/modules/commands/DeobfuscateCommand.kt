package io.github.mojira.arisa.modules.commands

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.urielsalis.mccrashlib.Crash
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.AttachmentUtils
import io.github.mojira.arisa.infrastructure.getDeobfName

private val MISSING_DEOBFUSCATION_ARGUMENTS = SimpleCommandExceptionType(
    LiteralMessage("Version (and crash report type) could not be detected; must be specified manually")
)

enum class CrashReportType {
    CLIENT,
    SERVER
}

class DeobfuscateCommand(private val attachmentUtils: AttachmentUtils) {

    @Suppress("ThrowsCount")
    operator fun invoke(
        issue: Issue,
        attachmentId: String,
        minecraftVersionId: String? = null,
        crashReportType: CrashReportType? = null
    ): Int {
        val attachment = issue.attachments.find { it.id == attachmentId }
            ?: throw CommandExceptions.NO_ATTACHMENT_WITH_ID.create(attachmentId)

        val deobfuscatedName = getDeobfName(attachment.name)
        if (issue.attachments.any { it.name == deobfuscatedName }) {
            throw CommandExceptions.ATTACHMENT_ALREADY_EXISTS.create(deobfuscatedName)
        }

        @Suppress("VariableNaming")
        var minecraftVersionId_ = minecraftVersionId
        var isClientCrash = when (crashReportType) {
            CrashReportType.CLIENT -> true
            CrashReportType.SERVER -> false
            else -> null
        }

        // If version or crash report type are not specified try to obtain them from crash report
        if (minecraftVersionId_ == null || isClientCrash == null) {
            val parsedCrashReport = attachmentUtils.processCrash(attachmentUtils.fetchAttachment(attachment))
                ?.crash as? Crash.Minecraft
                ?: throw MISSING_DEOBFUSCATION_ARGUMENTS.create()

            if (minecraftVersionId_ == null) {
                minecraftVersionId_ = parsedCrashReport.minecraftVersion
                    ?: throw MISSING_DEOBFUSCATION_ARGUMENTS.create()
            }
            if (isClientCrash == null) {
                isClientCrash = parsedCrashReport.isClient
            }
        }

        val deobfuscated = try {
            attachmentUtils.deobfuscate(
                String(attachment.getContent()),
                minecraftVersionId_,
                isClientCrash
            )
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            throw CommandExceptions.CommandExecutionException(
                "Deobfuscation of attachment with ID '${attachment.id}' failed: ${e.message}",
                e
            )
        }

        issue.addAttachment(deobfuscatedName, deobfuscated)
        return 1
    }
}
