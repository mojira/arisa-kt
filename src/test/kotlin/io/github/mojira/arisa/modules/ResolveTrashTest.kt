package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.modules.ResolveTrashModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class ResolveTrashTest : StringSpec({
    "should return OperationNotNeededModuleResponse when project is not TRASH" {
        val module = ResolveTrashModule()
        val request = Request("MC") { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid when ticket when ticket was open" {
        val module = ResolveTrashModule()
        val request = Request("TRASH") { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when resolving as invalid fails" {
        val module = ResolveTrashModule()
        val request = Request("TRASH") { RuntimeException().left() }

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
