package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Version
import io.github.mojira.arisa.modules.FutureVersionModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant

private val NOW = Instant.now()

class FutureVersionModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when affected versions are empty" {
        val module = FutureVersionModule()
        val releasedVersion = getVersion(true, false) { Unit.right() }
        val issue = getIssue(emptyList(), listOf(releasedVersion), { Unit.right() }) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when project versions are empty" {
        val module = FutureVersionModule()
        val futureVersion = getVersion(false, false) { Unit.right() }
        val issue = getIssue(listOf(futureVersion), emptyList(), { Unit.right() }) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when project versions are null" {
        val module = FutureVersionModule()
        val futureVersion = getVersion(false, false) { Unit.right() }
        val issue = getIssue(listOf(futureVersion), null, { Unit.right() }) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when project versions do not contain released versions" {
        val module = FutureVersionModule()
        val futureVersion = getVersion(false, false) { Unit.right() }
        val issue = getIssue(listOf(futureVersion), listOf(futureVersion), { Unit.right() }) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if no future version is marked affected" {
        val module = FutureVersionModule()
        val releasedVersion = getVersion(true, false) { Unit.right() }
        val issue = getIssue(listOf(releasedVersion), listOf(releasedVersion), { Unit.right() }) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if only an archived version is marked affected" {
        val module = FutureVersionModule()
        val archivedVersion = getVersion(false, true) { Unit.right() }
        val releasedVersion = getVersion(true, false) { Unit.right() }
        val issue = getIssue(listOf(archivedVersion), listOf(releasedVersion), { Unit.right() }) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should remove future versions" {
        val module = FutureVersionModule()
        val futureVersion = getVersion(false, false) { Unit.right() }
        val releasedVersion = getVersion(true, false) { Unit.right() }
        val issue = getIssue(listOf(futureVersion), listOf(releasedVersion), { Unit.right() }) { Unit.right() }

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when removing a version fails" {
        val module = FutureVersionModule()
        val futureVersion = getVersion(false, false) { RuntimeException().left() }
        val releasedVersion = getVersion(true, false) { Unit.right() }
        val issue = getIssue(listOf(futureVersion), listOf(releasedVersion), { Unit.right() }) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all exceptions when removing versions fails" {
        val module = FutureVersionModule()
        val futureVersion = getVersion(false, false) { RuntimeException().left() }
        val releasedVersion = getVersion(true, false) { Unit.right() }
        val issue = getIssue(listOf(futureVersion, futureVersion), listOf(releasedVersion), { Unit.right() }) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should return FailedModuleResponse when adding the latest version fails" {
        val module = FutureVersionModule()
        val futureVersion = getVersion(false, false) { Unit.right() }
        val releasedVersion = getVersion(true, false) { RuntimeException().left() }
        val issue = getIssue(listOf(futureVersion), listOf(releasedVersion), { Unit.right() }) { Unit.right() }

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when adding the comment fails" {
        val module = FutureVersionModule()
        val futureVersion = getVersion(false, false) { Unit.right() }
        val releasedVersion = getVersion(true, false) { Unit.right() }
        val issue = getIssue(listOf(futureVersion), listOf(releasedVersion), { Unit.right() }) { RuntimeException().left() }

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})

private fun getVersion(released: Boolean, archived: Boolean, execute: () -> Either<Throwable, Unit>) = Version(
    "",
    "",
    released,
    archived,
    NOW,
    execute
)
