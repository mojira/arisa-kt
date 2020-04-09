package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import net.rcarz.jiraclient.Issue

class CHKModuleTest : StringSpec({
    val ISSUE = mockk<Issue>()

    "should not process tickets without a confirmation status" {
        val module = CHKModule { Unit.right() }
        val request = CHKModuleRequest(ISSUE, null, null)

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should not process tickets with a Undefined confirmation status" {
        val module = CHKModule { Unit.right() }
        val request = CHKModuleRequest(ISSUE, null, "Undefined")

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should not process tickets with a Unconfirmed confirmation status" {
        val module = CHKModule { Unit.right() }
        val request = CHKModuleRequest(ISSUE, null, "Unconfirmed")

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should not process tickets with CHK already set" {
        val module = CHKModule { Unit.right() }
        val request = CHKModuleRequest(ISSUE, "chkField", "Confirmed")

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should process confirmed tickets" {
        val module = CHKModule { Unit.right() }
        val request = CHKModuleRequest(ISSUE, null, "Confirmed")

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when update fails" {
        val module = CHKModule { RuntimeException().left() }
        val request = CHKModuleRequest(ISSUE, null, "Confirmed")

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
