package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.modules.CHKModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class CHKModuleTest : StringSpec({
    "should not process tickets without a confirmation status" {
        val module = CHKModule()
        val request = Request(null, null) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should not process tickets with a Undefined confirmation status" {
        val module = CHKModule()
        val request = Request(null, "Undefined") { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should not process tickets with a Unconfirmed confirmation status" {
        val module = CHKModule()
        val request = Request(null, "Unconfirmed") { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should not process tickets with CHK already set" {
        val module = CHKModule()
        val request = Request("chkField", "Confirmed") { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should process confirmed tickets" {
        val module = CHKModule()
        val request = Request(null, "Confirmed") { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when update fails" {
        val module = CHKModule()
        val request = Request(null, "Confirmed") { RuntimeException().left() }

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
