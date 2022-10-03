package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.right
import io.github.mojira.arisa.ExecutionTimeframe
import io.github.mojira.arisa.domain.Comment
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.User
import org.slf4j.LoggerFactory
import java.time.Instant

class ShadowbanModule : Module {
    private val log = LoggerFactory.getLogger("ShadowbanModule")

    override fun invoke(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> {
        // This implementation should never be run, but if it is anyway, just print to log and move on
        log.error("Tried to invoke ShadowbanModule without details about shadowbans")
        return Either.left(OperationNotNeededModuleResponse)
    }

    override fun invoke(
        issue: Issue,
        timeframe: ExecutionTimeframe
    ): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            val bugReportRemoved = checkReporterForShadowban(timeframe)
            val removedComments = checkCommentsForShadowban(timeframe)
            val removedAttachments = checkAttachmentsForShadowban(timeframe)

            if (bugReportRemoved) {
                log.info("[ShadowbanModule] Put $key into the spam bin")
            }
            if (removedComments > 0) {
                log.info("[ShadowbanModule] Removed $removedComments comment(s) from $key")
            }
            if (removedAttachments > 0) {
                log.info("[ShadowbanModule] Removed $removedAttachments attachment(s) from $key")
            }

            val actionTaken = bugReportRemoved || removedComments > 0 || removedAttachments > 0

            assertTrue(actionTaken).bind()

            ModuleResponse.right().bind()
        }
    }

    private fun Issue.checkReporterForShadowban(timeframe: ExecutionTimeframe): Boolean {
        val reporterIsShadowbanned = reporter?.let { user ->
            user.isNotVolunteer() && (timeframe.shadowbans[user.name]?.banTimeContains(created) ?: false)
        } ?: false

        if (reporterIsShadowbanned) putInSpamBin()

        return reporterIsShadowbanned
    }

    private fun Issue.checkCommentsForShadowban(timeframe: ExecutionTimeframe): Int =
        comments
            .filter { it.isNotStaffRestricted() }
            .filter { it.author.isNotVolunteer() }
            .filter {
                timeframe.shadowbans[it.author.name]?.banTimeContains(it.created) ?: false
            }
            .map { it.restrict("${it.body}\n\n_Removed by Arisa -- User is shadowbanned_") }
            .size

    private fun Issue.checkAttachmentsForShadowban(timeframe: ExecutionTimeframe): Int =
        attachments
            .filter { it.uploader?.isNotVolunteer() ?: false }
            .filter {
                it.uploader?.let { uploader ->
                    timeframe.shadowbans[uploader.name]?.banTimeContains(it.created)
                } ?: false
            }
            .map { it.remove() }
            .size

    private fun User.isNotVolunteer() =
        getGroups()?.none { listOf("helper", "global-moderators", "staff").contains(it) } ?: true

    private fun Comment.isNotStaffRestricted() =
        visibilityType != "group" || visibilityValue != "staff"

    private fun Issue.putInSpamBin() {
        changeReporter("SpamBin")
        if (securityLevel == null) setPrivate()
        if (resolution == null || resolution == "Unresolved") resolveAsInvalid()
    }
}
