package io.github.mojira.arisa.modules

import arrow.core.right
import io.github.mojira.arisa.infrastructure.jira.updatePriority
import io.github.mojira.arisa.utils.RIGHT_NOW
import io.github.mojira.arisa.utils.mockChangeLogItem
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.temporal.ChronoUnit

class RevokePriorityModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when Ticket has no priority and priority was never set" {
        val module = RevokePriorityModule()
        val issue = mockIssue(
            priority = "None"
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket has a priority and was changed by staff" {
        val module = RevokePriorityModule()
        val changeLogItem = io.github.mojira.arisa.modules.mockChangeLogItem { listOf("staff") }
        val issue = mockIssue(
            priority = "Important",
            changeLog = listOf(changeLogItem)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket has a priority and was changed by global-moderator" {
        val module = RevokePriorityModule()
        val changeLogItem = io.github.mojira.arisa.modules.mockChangeLogItem { listOf("global-moderators") }
        val issue = mockIssue(
            priority = "Important",
            changeLog = listOf(changeLogItem)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket priority was set more than a day ago by a user who is no longer staff" {
        val module = RevokePriorityModule()
        val changeLogItem = io.github.mojira.arisa.modules.mockChangeLogItem(Instant.now().minus(2, ChronoUnit.DAYS))
        val issue = mockIssue(
            priority = "Important",
            changeLog = listOf(changeLogItem)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when Ticket has priority and groups are unknown" {
        val module = RevokePriorityModule()
        val changeLogItem = io.github.mojira.arisa.modules.mockChangeLogItem { null }
        val issue = mockIssue(
            priority = "Important",
            changeLog = listOf(changeLogItem)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when multiple permitted changed the mojang priority" {
        val module = RevokePriorityModule()
        val permittedChange = io.github.mojira.arisa.modules.mockChangeLogItem { listOf("staff") }
        val otherPermittedChange = mockChangeLogItem(value = "None") { listOf("global-moderators") }
        val issue = mockIssue(
            priority = "None",
            changeLog = listOf(permittedChange, otherPermittedChange)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when ticket has no priority and priority was unset" {
        val module = RevokePriorityModule()
        val permittedChange = io.github.mojira.arisa.modules.mockChangeLogItem { listOf("staff") }
        val otherPermittedChange = mockChangeLogItem(value = "") { listOf("global-moderators") }
        val issue = mockIssue(
            priority = "None",
            changeLog = listOf(permittedChange, otherPermittedChange)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when priority is null and was unset" {
        val module = RevokePriorityModule()
        val permittedChange = io.github.mojira.arisa.modules.mockChangeLogItem { listOf("staff") }
        val otherPermittedChange = mockChangeLogItem(value = "") { listOf("global-moderators") }
        val issue = mockIssue(
            changeLog = listOf(permittedChange, otherPermittedChange)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when priority is empty and was unset" {
        val module = RevokePriorityModule()
        val permittedChange = io.github.mojira.arisa.modules.mockChangeLogItem { listOf("staff") }
        val otherPermittedChange = mockChangeLogItem(value = "") { listOf("global-moderators") }
        val issue = mockIssue(
            priority = "",
            changeLog = listOf(permittedChange, otherPermittedChange)
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should set priority to None when ticket was created with a priority" {
        var changedPriority = ""

        val module = RevokePriorityModule()
        val issue = mockIssue(
            priority = "Important",
            updatePriority = { changedPriority = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        changedPriority.shouldBe("None")
    }

    "should set to None when there is a changelog entry unrelated to mojang priority" {
        var changedPriority = ""

        val module = RevokePriorityModule()
        val changeLogItem =
            io.github.mojira.arisa.modules.mockChangeLogItem(field = "Totally Not Priority") { listOf("staff") }
        val issue = mockIssue(
            priority = "Important",
            changeLog = listOf(changeLogItem),
            updatePriority = { changedPriority = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        changedPriority.shouldBe("None")
    }

    "should set to None when ticket priority was changed by a non-permitted" {
        var changedPriority = ""

        val module = RevokePriorityModule()
        val changeLogItem = io.github.mojira.arisa.modules.mockChangeLogItem()
        val issue = mockIssue(
            priority = "Important",
            changeLog = listOf(changeLogItem),
            updatePriority = { changedPriority = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        changedPriority.shouldBe("None")
    }

    "should set back to priority set by permitted, when regular user changes mojang priority" {
        var changedPriority = ""

        val module = RevokePriorityModule()
        val permittedChange = io.github.mojira.arisa.modules.mockChangeLogItem { listOf("staff") }
        val userChange = mockChangeLogItem(value = "None") { listOf("users") }
        val issue = mockIssue(
            priority = "None",
            changeLog = listOf(permittedChange, userChange),
            updatePriority = { changedPriority = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        changedPriority.shouldBe("Important")
    }

    "should set back to previous status when regular user changes confirmation status set by someone who no longer is a volunteer" {
        var changedPriority = ""

        val module = RevokePriorityModule()
        val permittedChange = io.github.mojira.arisa.modules.mockChangeLogItem(Instant.now().minus(2, ChronoUnit.DAYS))
        val userChange = mockChangeLogItem(value = "None") { listOf("users") }
        val issue = mockIssue(
            priority = "None",
            changeLog = listOf(permittedChange, userChange),
            updatePriority = { changedPriority = it; Unit.right() }
        )

        val result = module(issue, RIGHT_NOW)

        result.shouldBeRight(ModuleResponse)
        changedPriority.shouldBe("Important")
    }
})

private fun mockChangeLogItem(
    created: Instant = RIGHT_NOW,
    field: String = "Mojang Priority",
    value: String = "Important",
    getAuthorGroups: () -> List<String>? = { emptyList() }
) = mockChangeLogItem(
    created = created,
    field = field,
    changedToString = value,
    getAuthorGroups = getAuthorGroups
)
