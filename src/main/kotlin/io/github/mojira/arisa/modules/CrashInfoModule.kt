package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import arrow.syntax.function.partially2
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.AttachmentUtils
import com.urielsalis.mccrashlib.Crash
import com.urielsalis.mccrashlib.CrashReader
import com.urielsalis.mccrashlib.deobfuscator.getSafeChildPath
import java.nio.file.Files
import java.time.Instant

class CrashInfoModule(
    private val crashReportExtensions: List<String>,
    private val crashReader: CrashReader
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val crashes = AttachmentUtils(crashReportExtensions, crashReader).extractCrashesFromAttachments(issue)
            val newCrashes = assertContainsNewCrash(crashes, lastRun).bind()
            uploadDeobfuscatedCrashes(issue, newCrashes)

            newCrashes
                .filter { it.second is Crash.Minecraft }
                .filterNot { it.first.name.endsWith("deobfuscated.txt") }
                .forEach {
                    if (!description!!.contains("""\{code.*${it.first.name}]}(\S|\s)*\{code}""".toRegex())) {
                        updateDescription(
                            description + "\n\n" +
                                    generateCrashMessage(it.first.name, it.second as Crash.Minecraft)
                        )
                    }
                }
        }
    }

    private fun generateCrashMessage(name: String, minecraft: Crash.Minecraft) =
        "{code:title=(${minecraft.minecraftVersion}) [^$name]}\n\n" +
            "Description: ${minecraft.exception.split("\n").first()}\n\n" +
            "Exception: ${minecraft.exception}\n\n" +
            (if (minecraft.deobfException != null) "Deobfuscated: ${minecraft.deobfException}\n" else "\n") +
            "{code}\n"

    private fun uploadDeobfuscatedCrashes(issue: Issue, crashes: List<Pair<AttachmentUtils.TextDocument, Crash>>) {
        val minecraftCrashesWithDeobf = crashes
            .map { it.first.name to it.second }
            .filter { it.second is Crash.Minecraft }
            .map { it.first to (it.second as Crash.Minecraft).deobf }
            .filter { it.second != null }
            .filterNot {
                issue.attachments.any { attachment ->
                    attachment.name == getDeobfName(it.first) || attachment.name.endsWith(
                        "deobfuscated.txt"
                    )
                }
            }
        minecraftCrashesWithDeobf.forEach {
            val tempDir = Files.createTempDirectory("arisa-crash-upload").toFile()
            val safePath = getSafeChildPath(tempDir, getDeobfName(it.first))
            if (safePath == null) {
                tempDir.delete()
            } else {
                safePath.writeText(it.second!!)
                issue.addAttachment(safePath) {
                    // Once uploaded, delete the temp directory containing the crash report
                    tempDir.deleteRecursively()
                }
            }
        }
    }

    private fun getDeobfName(name: String): String =
        "${name.substringBeforeLast(".").replace("\\", "").replace("/", "")}-deobfuscated.txt"

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
}
