package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.modules.FutureVersionModule.Request
import io.github.mojira.arisa.modules.FutureVersionModule.Version
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class FutureVersionModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when affected versions are empty" {
        val module = FutureVersionModule()
        val releasedVersion = Version(true, false) { Unit.right() }
        val request = Request(emptyList(), listOf(releasedVersion)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when project versions are empty" {
        val module = FutureVersionModule()
        val futureVersion = Version(false, false) { Unit.right() }
        val request = Request(listOf(futureVersion), emptyList()) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when project versions are null" {
        val module = FutureVersionModule()
        val futureVersion = Version(false, false) { Unit.right() }
        val request = Request(listOf(futureVersion), null) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when project versions do not contain released versions" {
        val module = FutureVersionModule()
        val futureVersion = Version(false, false) { Unit.right() }
        val request = Request(listOf(futureVersion), listOf(futureVersion)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if no future version is marked affected" {
        val module = FutureVersionModule()
        val releasedVersion = Version(true, false) { Unit.right() }
        val request = Request(listOf(releasedVersion), listOf(releasedVersion)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if only an archived version is marked affected" {
        val module = FutureVersionModule()
        val archivedVersion = Version(false, true) { Unit.right() }
        val releasedVersion = Version(true, false) { Unit.right() }
        val request = Request(listOf(archivedVersion), listOf(releasedVersion)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should remove future versions" {
        val module = FutureVersionModule()
        val futureVersion = Version(false, false) { Unit.right() }
        val releasedVersion = Version(true, false) { Unit.right() }
        val request = Request(listOf(futureVersion), listOf(releasedVersion)) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when removing a version fails" {
        val module = FutureVersionModule()
        val futureVersion = Version(false, false) { RuntimeException().left() }
        val releasedVersion = Version(true, false) { Unit.right() }
        val request = Request(listOf(futureVersion), listOf(releasedVersion)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all exceptions when removing versions fails" {
        val module = FutureVersionModule()
        val futureVersion = Version(false, false) { RuntimeException().left() }
        val releasedVersion = Version(true, false) { Unit.right() }
        val request = Request(listOf(futureVersion, futureVersion), listOf(releasedVersion)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should return FailedModuleResponse when adding the latest version fails" {
        val module = FutureVersionModule()
        val futureVersion = Version(false, false) { Unit.right() }
        val releasedVersion = Version(true, false) { RuntimeException().left() }
        val request = Request(listOf(futureVersion), listOf(releasedVersion)) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when adding the comment fails" {
        val module = FutureVersionModule()
        val futureVersion = Version(false, false) { Unit.right() }
        val releasedVersion = Version(true, false) { Unit.right() }
        val request = Request(listOf(futureVersion), listOf(releasedVersion)) { RuntimeException().left() }

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
