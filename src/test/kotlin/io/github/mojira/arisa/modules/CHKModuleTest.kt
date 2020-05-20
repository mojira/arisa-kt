package io.github.mojira.arisa.modules

import arrow.core.left
import io.github.mojira.arisa.utils.getIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant

private val NOW = Instant.now()

class CHKModuleTest : StringSpec({
    "should not process tickets without a confirmation status" {
        val module = CHKModule()
        val issue = getIssue()

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should not process tickets with a Undefined confirmation status" {
        val module = CHKModule()
        val issue = getIssue(
            confirmationStatus = "Undefined"
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should not process tickets with a Unconfirmed confirmation status" {
        val module = CHKModule()
        val issue = getIssue(
            confirmationStatus = "Unconfirmed"
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should not process tickets with CHK already set" {
        val module = CHKModule()
        val issue = getIssue(
            chk = "chkField",
            confirmationStatus = "Confirmed"
        )

        val result = module(issue, NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should process confirmed tickets" {
        val module = CHKModule()
        val issue = getIssue(
            confirmationStatus = "Confirmed"
        )

        val result = module(issue, NOW)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when update fails" {
        val module = CHKModule()
        val issue = getIssue(
            confirmationStatus = "Confirmed",
            updateCHK = { RuntimeException().left() }
        )

        val result = module(issue, NOW)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
