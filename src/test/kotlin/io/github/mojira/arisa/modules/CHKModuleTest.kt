package io.github.mojira.arisa.modules

import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import java.time.Instant

private val NOW = Instant.now()

class CHKModuleTest : StringSpec({
    "should not process tickets without a confirmation status" {
        val module = CHKModule()
        val issue = mockIssue()

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should not process tickets with a Undefined confirmation status" {
        val module = CHKModule()
        val issue = mockIssue(
            confirmationStatus = "Undefined"
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should not process tickets with a Unconfirmed confirmation status" {
        val module = CHKModule()
        val issue = mockIssue(
            confirmationStatus = "Unconfirmed"
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should not process tickets with CHK already set" {
        val module = CHKModule()
        val issue = mockIssue(
            chk = "chkField",
            confirmationStatus = "Confirmed"
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should process confirmed tickets" {
        val module = CHKModule()
        val issue = mockIssue(
            confirmationStatus = "Confirmed"
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
    }
})
