package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.domain.Link
import java.time.Instant

class ConfirmParentModule(
    private val confirmationStatusWhitelist: List<String>,
    private val targetConfirmationStatus: String,
    private val linkedThreshold: Double
) : Module() {
    override fun execute(issue: Issue, lastRun: Instant): Either<ModuleError, ModuleResponse> = with(issue) {
        Either.fx {
            assertConfirmationStatusWhitelisted(confirmationStatus, confirmationStatusWhitelist).bind()
            assertTrue(isDuplicatedEnough(issue).bind()).bind()
            confirmationStatus = targetConfirmationStatus
        }
    }

    private fun isDuplicatedEnough(issue: Issue): Either<ModuleError, Boolean> = Either.fx {
        val reporters = mutableSetOf(issue.reporter?.name)
        var amount = 0
        issue.links
            .filter(::isDuplicatedLink)
            .forEach {
                val child = it.issue.issue.get()
                if (child.reporter?.name !in reporters) {
                    if (++amount >= linkedThreshold) {
                        return@fx true
                    }
                    reporters.add(child.reporter?.name)
                }
            }
        false
    }

    private fun isDuplicatedLink(link: Link): Boolean = link.type == "Duplicate" && !link.outwards

    private fun assertConfirmationStatusWhitelisted(status: String?, whitelist: List<String>) =
        if ((status.getOrDefault("Unconfirmed")) in whitelist) {
            Unit.right()
        } else {
            OperationNotNeededModuleResponse.left()
        }
}
