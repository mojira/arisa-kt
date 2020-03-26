package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class EmptyModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is a attachment and desc and env are correct" {
        val module = EmptyModule({ Unit.right() }, { Unit.right() })
        val request = EmptyModuleRequest(1, "asddsa", "asddsa")

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no attachment and desc and env are correct" {
        val module = EmptyModule({ Unit.right() }, { Unit.right() })
        val request = EmptyModuleRequest(0, "asddsa", "asddsa")

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is a attachment and no desc or env" {
        val module = EmptyModule({ Unit.right() }, { Unit.right() })
        val request = EmptyModuleRequest(1, null, null)

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no attachment and no desc and env is correct" {
        val module = EmptyModule({ Unit.right() }, { Unit.right() })
        val request = EmptyModuleRequest(0, null, "asddsa")

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no attachment and desc is correct and no env" {
        val module = EmptyModule({ Unit.right() }, { Unit.right() })
        val request = EmptyModuleRequest(0, "asdasd", null)

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid when there is no attachment and no desc or env" {
        val module = EmptyModule({ Unit.right() }, { Unit.right() })
        val request = EmptyModuleRequest(0, null, null)

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should resolve as invalid when there is no attachment and desc is default and env is empty" {
        val module = EmptyModule({ Unit.right() }, { Unit.right() })
        val request = EmptyModuleRequest(0, DESCDEFAULT, null)

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should resolve as invalid when there is no attachment and desc is empty and env is default" {
        val module = EmptyModule({ Unit.right() }, { Unit.right() })
        val request = EmptyModuleRequest(0, null, ENVDEFAULT)

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should resolve as invalid when there is no attachment and desc is too short and env is too short" {
        val module = EmptyModule({ Unit.right() }, { Unit.right() })
        val request = EmptyModuleRequest(0, "asd", "asd")

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when resolving fails" {
        val module = EmptyModule({ RuntimeException().left() }, { Unit.right() })
        val request = EmptyModuleRequest(0, "asd", "asd")

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when adding comment fails" {
        val module = EmptyModule({ Unit.right() }, { RuntimeException().left() })
        val request = EmptyModuleRequest(0, "asd", "asd")

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
