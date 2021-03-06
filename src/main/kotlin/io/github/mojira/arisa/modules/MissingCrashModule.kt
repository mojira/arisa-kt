package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import io.github.mojira.arisa.domain.CommentOptions
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.services.AttachmentUtils
import me.urielsalis.mccrashlib.CrashReader
import java.time.Instant

class MissingCrashModule(
    private val crashReportExtensions: List<String>,
    private val crashReader: CrashReader,
    private val message: String
) : Module {
    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertEquals(confirmationStatus ?: "Unconfirmed", "Unconfirmed").bind()
            assertEquals(status, "Open").bind()
            assertNull(priority).bind()
            assertContains(description, "crash").bind()

            val crashes = AttachmentUtils(crashReportExtensions, crashReader).extractCrashesFromAttachments(issue)

            assertEmpty(crashes).bind()

            resolveAsAwaitingResponse()
            addComment(CommentOptions(message))
        }
    }
}
