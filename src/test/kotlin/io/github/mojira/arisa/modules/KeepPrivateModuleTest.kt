package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class KeepPrivateModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when keep private tag is null" {
        val module = KeepPrivateModule(null)
        val request = KeepPrivateModule.Request("private", "private", listOf("MEQS_KEEP_PRIVATE"), { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when comments are empty" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE")
        val request = KeepPrivateModule.Request(null, "private", emptyList(), { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when no comment contains private tag" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE")
        val request = KeepPrivateModule.Request(null, "private", listOf("Hello world!"), { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when security level is set to private" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE")
        val request = KeepPrivateModule.Request("private", "private", listOf("MEQS_KEEP_PRIVATE"), { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should set to private when security level is null" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE")
        val request = KeepPrivateModule.Request(null, "private", listOf("MEQS_KEEP_PRIVATE"), { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should set to private when security level is not private" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE")
        val request = KeepPrivateModule.Request("not private", "private", listOf("MEQS_KEEP_PRIVATE"), { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when setting security level fails" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE")
        val request = KeepPrivateModule.Request(null, "private", listOf("MEQS_KEEP_PRIVATE"), { RuntimeException().left() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when posting comment" {
        val module = KeepPrivateModule("MEQS_KEEP_PRIVATE")
        val request = KeepPrivateModule.Request(null, "private", listOf("MEQS_KEEP_PRIVATE"), { Unit.right() }, { RuntimeException().left() })

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
