package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class PiracyModuleTest : StringSpec({
    val PIRACYSIGNATURES = listOf("test", "signature with whitespaces")

    "should return OperationNotNeededModuleResponse when there is no description, summary or environment" {
        val module = PiracyModule({ Unit.right() }, { Unit.right() }, PIRACYSIGNATURES)
        val request = PiracyModuleRequest(null, null, null)

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when description, summary and environment are empty" {
        val module = PiracyModule({ Unit.right() }, { Unit.right() }, PIRACYSIGNATURES)
        val request = PiracyModuleRequest("", "", "")

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when piracy signatures is empty" {
        val module = PiracyModule({ Unit.right() }, { Unit.right() }, listOf())
        val request = PiracyModuleRequest("", "", "test")

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when no signature matches" {
        val module = PiracyModule({ Unit.right() }, { Unit.right() }, listOf())
        val request = PiracyModuleRequest("else", "nope", "something")

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid if description contains a piracy signature" {
        val module = PiracyModule({ Unit.right() }, { Unit.right() }, PIRACYSIGNATURES)
        val request = PiracyModuleRequest("", "", "test")

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if description contains a piracy signature but not as a full word" {
        val module = PiracyModule({ Unit.right() }, { Unit.right() }, PIRACYSIGNATURES)
        val request = PiracyModuleRequest("", "", "testusername")

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid if summary contains a piracy signature" {
        val module = PiracyModule({ Unit.right() }, { Unit.right() }, PIRACYSIGNATURES)
        val request = PiracyModuleRequest("", "test", "")

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should resolve as invalid if environment contains a piracy signature" {
        val module = PiracyModule({ Unit.right() }, { Unit.right() }, PIRACYSIGNATURES)
        val request = PiracyModuleRequest("test", "", "")

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should resolve as invalid if environment contains a piracy signature using whitespaces" {
        val module = PiracyModule({ Unit.right() }, { Unit.right() }, PIRACYSIGNATURES)
        val request = PiracyModuleRequest("signature with whitespaces", "", "")

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when resolving as invalid fails" {
        val module = PiracyModule({ RuntimeException().left() }, { Unit.right() }, PIRACYSIGNATURES)
        val request = PiracyModuleRequest("test", "", "")

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when adding comment fails" {
        val module = PiracyModule({ Unit.right() }, { RuntimeException().left() }, PIRACYSIGNATURES)
        val request = PiracyModuleRequest("test", "", "")

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
