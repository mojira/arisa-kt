package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.modules.EmptyModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class EmptyModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when ticket was created before the last run" {
        val module = EmptyModule()
        val request = Request(
            0,
            1,
            0,
            null,
            null,
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is a attachment and desc and env are correct" {
        val module = EmptyModule()
        val request = Request(
            1,
            0,
            1,
            "asddsa",
            "asddsa",
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no attachment and desc and env are correct" {
        val module = EmptyModule()
        val request = Request(
            1,
            0,
            0,
            "asddsa",
            "asddsa",
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is a attachment and no desc or env" {
        val module = EmptyModule()
        val request = Request(
            1,
            0,
            1,
            null,
            null,
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no attachment and no desc and env is correct" {
        val module = EmptyModule()
        val request = Request(
            1,
            0,
            0,
            null,
            "asddsa",
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no attachment and desc is correct and no env" {
        val module = EmptyModule()
        val request = Request(
            1,
            0,
            0,
            "asdasd",
            null,
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid when there is no attachment and no desc or env" {
        val module = EmptyModule()
        val request = Request(
            1,
            0,
            0,
            null,
            null,
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should resolve as invalid when there is no attachment and desc is default and env is empty" {
        val module = EmptyModule()
        val request = Request(
            1,
            0,
            0,
            DESC_DEFAULT,
            null,
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should resolve as invalid when there is no attachment and desc is empty and env is default" {
        val module = EmptyModule()
        val request = Request(
            1,
            0,
            0,
            null,
            ENV_DEFAULT,
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should resolve as invalid when there is no attachment and desc is too short and env is too short" {
        val module = EmptyModule()
        val request = Request(
            1,
            0,
            0,
            "asd",
            "asd",
            { Unit.right() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when resolving fails" {
        val module = EmptyModule()
        val request = Request(
            1,
            0,
            0,
            "asd",
            "asd",
            { RuntimeException().left() },
            { Unit.right() }
        )

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when adding comment fails" {
        val module = EmptyModule()
        val request = Request(
            1,
            0,
            0,
            "asd",
            "asd",
            { Unit.right() },
            { RuntimeException().left() }
        )

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
