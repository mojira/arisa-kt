package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.domain.Link
import io.github.mojira.arisa.domain.LinkedIssue
import io.github.mojira.arisa.domain.Version
import io.github.mojira.arisa.modules.AbstractTransferFieldModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.Instant

private val NOW = Instant.now()
private val VERSION_1 = getVersion("v1", NOW.minusSeconds(300))
private val VERSION_2 = getVersion("v2", NOW.minusSeconds(200))
private val VERSION_3 = getVersion("v3", NOW.minusSeconds(100))

class TransferVersionsModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there are no issue links" {
        val module = TransferVersionsModule()
        val request = Request<List<Version>, String>("MC-1", emptyList(), listOf(VERSION_1))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no duplicates link" {
        val module = TransferVersionsModule()
        val link = Link(
            "Relates",
            true,
            LinkedIssue<List<Version>, String>("MC-1", "Open", { Unit.right() }, { emptyList<Version>().right() })
        ) { RuntimeException().left() }
        val request = Request("MC-1", listOf(link), listOf(VERSION_1))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when there is no outgoing duplicates link" {
        val module = TransferVersionsModule()
        val link = Link(
            "Duplicate",
            false,
            LinkedIssue<List<Version>, String>("MC-1", "Open", { Unit.right() }, { emptyList<Version>().right() })
        ) { RuntimeException().left() }
        val request = Request("MC-1", listOf(link), listOf(VERSION_1))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the parent is resolved" {
        val module = TransferVersionsModule()
        val link = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Version>, String>("MC-1", "Resolved", { Unit.right() }, { emptyList<Version>().right() })
        ) { RuntimeException().left() }
        val request = Request("MC-1", listOf(link), listOf(VERSION_1))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the issue has no affected versions" {
        val module = TransferVersionsModule()
        val link = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Version>, String>("MC-1", "Open", { Unit.right() }, { emptyList<Version>().right() })
        ) { RuntimeException().left() }
        val request = Request("MC-1", listOf(link), emptyList())

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the parent already has all versions" {
        val module = TransferVersionsModule()
        val link = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Version>, String>("MC-1", "Open", { Unit.right() }, { listOf(VERSION_1).right() })
        ) { RuntimeException().left() }
        val request = Request("MC-1", listOf(link), listOf(VERSION_1))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the parent is from a different project" {
        val module = TransferVersionsModule()
        val link = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Version>, String>("MCL-1", "Open", { Unit.right() }, { emptyList<Version>().right() })
        ) { RuntimeException().left() }
        val request = Request("MC-1", listOf(link), listOf(VERSION_1))

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when the version is released before the parent's oldest version (#229)" {
        val module = TransferVersionsModule()
        val link = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Version>, String>(
                "MC-1",
                "Open",
                { Unit.right() },
                { listOf(VERSION_2).right() }
            )
        ) { RuntimeException().left() }
        val request = Request("MC-1", listOf(link), listOf(VERSION_1))
        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should transfer missing versions to open parents" {
        val module = TransferVersionsModule()
        val link = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Version>, String>("MC-1", "Open", { Unit.right() }, { emptyList<Version>().right() })
        ) { RuntimeException().left() }
        val request = Request("MC-1", listOf(link), listOf(VERSION_1))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should transfer missing versions to reopened parents" {
        val module = TransferVersionsModule()
        val link = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Version>, String>("MC-1", "Reopened", { Unit.right() }, { emptyList<Version>().right() })
        ) { RuntimeException().left() }
        val request = Request("MC-1", listOf(link), listOf(VERSION_1))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should add all versions to parent" {
        var firstVersionAdded = false
        var secondVersionAdded = false
        val module = TransferVersionsModule()
        val link = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Version>, String>(
                "MC-1",
                "Open",
                { v ->
                    when (v) {
                        "v1" -> firstVersionAdded = true
                        "v2" -> secondVersionAdded = true
                    }
                    Unit.right()
                },
                { emptyList<Version>().right() }
            )
        ) { RuntimeException().left() }
        val request = Request("MC-1", listOf(link), listOf(VERSION_1, VERSION_2))
        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        firstVersionAdded.shouldBeTrue()
        secondVersionAdded.shouldBeTrue()
    }

    "should only add versions released after the oldest version to parent (#229)" {
        var version1Added = false
        var version3Added = false
        val module = TransferVersionsModule()
        val link = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Version>, String>(
                "MC-1",
                "Open",
                { v ->
                    when (v) {
                        "v1" -> version1Added = true
                        "v3" -> version3Added = true
                    }
                    Unit.right()
                },
                { listOf(VERSION_2).right() }
            )
        ) { RuntimeException().left() }
        val request = Request("MC-1", listOf(link), listOf(VERSION_1, VERSION_3))
        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        version1Added.shouldBeFalse()
        version3Added.shouldBeTrue()
    }

    "should add versions to all parents" {
        var addedToFirstParent = false
        var addedToSecondParent = false
        val module = TransferVersionsModule()
        val link1 = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Version>, String>(
                "MC-1",
                "Open",
                {
                    addedToFirstParent = true
                    Unit.right()
                },
                { emptyList<Version>().right() }
            )
        ) { RuntimeException().left() }

        val link2 = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Version>, String>(
                "MC-1",
                "Open",
                {
                    addedToSecondParent = true
                    Unit.right()
                },
                { emptyList<Version>().right() }
            )
        ) { RuntimeException().left() }

        val request = Request("MC-1", listOf(link1, link2), listOf(VERSION_1))

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
        addedToFirstParent.shouldBeTrue()
        addedToSecondParent.shouldBeTrue()
    }

    "should return FailedModuleResponse when adding a version fails" {
        val module = TransferVersionsModule()
        val link = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Version>, String>("MC-1", "Open", { RuntimeException().left() }, { emptyList<Version>().right() })
        ) { RuntimeException().left() }
        val request = Request("MC-1", listOf(link), listOf(VERSION_1))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all errors when adding multiple versions fails" {
        val module = TransferVersionsModule()
        val link = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Version>, String>("MC-1", "Open", { RuntimeException().left() }, { emptyList<Version>().right() })
        ) { RuntimeException().left() }
        val request = Request("MC-1", listOf(link), listOf(VERSION_1, VERSION_2))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }

    "should return FailedModuleResponse when getting an issue fails" {
        val module = TransferVersionsModule()
        val link = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Version>, String>("MC-1", "Open", { Unit.right() }, { RuntimeException().left() })
        ) { RuntimeException().left() }
        val request = Request("MC-1", listOf(link), listOf(VERSION_1))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse with all errors when getting an issue fails" {
        val module = TransferVersionsModule()
        val link = Link(
            "Duplicate",
            true,
            LinkedIssue<List<Version>, String>("MC-1", "Open", { Unit.right() }, { RuntimeException().left() })
        ) { RuntimeException().left() }
        val request = Request("MC-1", listOf(link, link), listOf(VERSION_1))

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 2
    }
})

private fun getVersion(name: String, releaseDate: Instant = NOW) = Version(
    name,
    released = true,
    archived = false,
    releaseDate = releaseDate
) { Unit.right() }
