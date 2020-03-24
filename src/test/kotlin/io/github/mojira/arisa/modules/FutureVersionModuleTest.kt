package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.kotlintest.assertions.arrow.either.shouldBeLeft
import io.kotlintest.assertions.arrow.either.shouldBeRight
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.mockk.every
import io.mockk.mockk
import net.rcarz.jiraclient.Version

class FutureVersionModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when versions are empty" {
        val module = FutureVersionModule({ Unit.right() }, { Unit.right() }, { Unit.right() })
        val request = FutureVersionModuleRequest(emptyList())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for released versions" {
        val module = FutureVersionModule({ Unit.right() }, { Unit.right() }, { Unit.right() })
        val version = mockVersion(true, false)
        val request = FutureVersionModuleRequest(listOf(version))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse for archived versions" {
        val module = FutureVersionModule({ Unit.right() }, { Unit.right() }, { Unit.right() })
        val version = mockVersion(false, true)
        val request = FutureVersionModuleRequest(listOf(version))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should remove future versions" {
        val module = FutureVersionModule({ Unit.right() }, { Unit.right() }, { Unit.right() })
        val version = mockVersion(false, false)
        val request = FutureVersionModuleRequest(listOf(version))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when removing a version fails" {
        val module = FutureVersionModule({ RuntimeException().left() }, { Unit.right() }, { Unit.right() })
        val version = mockVersion(false, false)
        val request = FutureVersionModuleRequest(listOf(version))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all exceptions when removing versions fails" {
        val module = FutureVersionModule({ RuntimeException().left() }, { Unit.right() }, { Unit.right() })
        val version = mockVersion(false, false)
        val request = FutureVersionModuleRequest(listOf(version, version))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should return FailedModuleResponse when adding the latest version fails" {
        val module = FutureVersionModule({ Unit.right() }, { RuntimeException().left() }, { Unit.right() })
        val version = mockVersion(false, false)
        val request = FutureVersionModuleRequest(listOf(version))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when adding the comment fails" {
        val module = FutureVersionModule({ Unit.right() }, { Unit.right() }, { RuntimeException().left() })
        val version = mockVersion(false, false)
        val request = FutureVersionModuleRequest(listOf(version))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})

private fun mockVersion(isReleased: Boolean, isArchived: Boolean): Version {
    val version = mockk<Version>()
    every { version.isReleased } returns isReleased
    every { version.isArchived } returns isArchived
    return version
}
