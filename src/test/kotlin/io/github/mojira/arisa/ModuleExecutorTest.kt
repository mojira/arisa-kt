package io.github.mojira.arisa

import arrow.core.Either
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.Cache
import io.github.mojira.arisa.infrastructure.IssueUpdateContextCache
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.ModuleError
import io.github.mojira.arisa.modules.ModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import io.github.mojira.arisa.utils.mockIssue
import io.github.mojira.arisa.utils.mockProject
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.time.Instant

val moduleRegistryMock = mockk<ModuleRegistry>()
val failedModuleRegistryMock = mockk<ModuleRegistry>()

class ModuleExecutorTest : StringSpec({
    every { moduleRegistryMock.getModules() } returns listOf(
        ModuleRegistry.Entry(
            "mock",
            Arisa.Modules.Attachment,
            DEFAULT_JQL,
            { _, _ -> "mock" to Either.left(OperationNotNeededModuleResponse) }
        )
    )
    every { failedModuleRegistryMock.getModules() } returns listOf(
        ModuleRegistry.Entry(
            "mock",
            Arisa.Modules.Attachment,
            DEFAULT_JQL,
            { _, _ -> "mock" to Either.left(FailedModuleResponse()) }
        )
    )

    "should add failed tickets" {
        val moduleExecutor = getMockModuleExecutor(registry = failedModuleRegistryMock)

        val result = moduleExecutor.execute(Instant.now(), setOf("MC-1"))

        result.failedTickets shouldContain "MC-1"
        result.successful shouldBe true
    }

    "should not add succesful tickets" {
        val moduleExecutor = getMockModuleExecutor(registry = moduleRegistryMock)

        val result = moduleExecutor.execute(Instant.now(), setOf("MC-1"))

        result.failedTickets.shouldBeEmpty()
        result.successful shouldBe true
    }

    "should fail execution if search issues fails" {
        val moduleExecutor =
            getMockModuleExecutor(
                registry = moduleRegistryMock,
                searchIssues = { _, _, _, _, _ -> throw RuntimeException() }
            )

        val result = moduleExecutor.execute(Instant.now(), setOf("MC-1"))

        result.failedTickets.shouldBeEmpty()
        result.successful shouldBe false
    }

    "should allow issues that pass all checks for CHK" {
        val moduleExecutor = getMockModuleExecutor(registry = moduleRegistryMock)

        val result = moduleExecutor.getIssues(
            Arisa.Modules.CHK,
            listOf(),
            "",
            Cache(),
            0,
            { Unit },
            Cache()
        )

        result.size shouldBe 1
    }

    "should allow issues that pass all checks for Reopen Awaiting" {
        val moduleExecutor = getMockModuleExecutor(
            registry = moduleRegistryMock,
            searchIssues = { _, _, _, _, _ -> listOf(mockIssue(resolution = "Awaiting Response")) }
        )

        val result = moduleExecutor.getIssues(
            Arisa.Modules.ReopenAwaiting,
            listOf(),
            "",
            Cache(),
            0,
            { Unit },
            Cache()
        )

        result.size shouldBe 1
    }

    "should filter issues where project is not in global whitelist" {
        val moduleExecutor = getMockModuleExecutor(
            registry = moduleRegistryMock,
            searchIssues = { _, _, _, _, _ -> listOf(mockIssue(project = mockProject(key = "TEST"))) }
        )

        val result = moduleExecutor.getIssues(
            Arisa.Modules.CHK,
            listOf(),
            "",
            Cache(),
            0,
            { Unit },
            Cache()
        )

        result.shouldBeEmpty()
    }

    "should filter issues where project is not in module whitelist" {
        val moduleExecutor = getMockModuleExecutor(
            registry = moduleRegistryMock,
            searchIssues = { _, _, _, _, _ -> listOf(mockIssue(project = mockProject(key = "MCD"))) }
        )

        val result = moduleExecutor.getIssues(
            Arisa.Modules.CHK,
            listOf(),
            "",
            Cache(),
            0,
            { Unit },
            Cache()
        )

        result.shouldBeEmpty()
    }

    "should filter issues where status is in excluded status" {
        val moduleExecutor = getMockModuleExecutor(
            registry = moduleRegistryMock,
            searchIssues = { _, _, _, _, _ -> listOf(mockIssue(status = "Postponed")) }
        )

        val result = moduleExecutor.getIssues(
            Arisa.Modules.CHK,
            listOf(),
            "",
            Cache(),
            0,
            { Unit },
            Cache()
        )

        result.shouldBeEmpty()
    }

    "should filter issues where resolution is in excluded resolution" {
        val moduleExecutor = getMockModuleExecutor(registry = moduleRegistryMock)

        val result = moduleExecutor.getIssues(
            Arisa.Modules.ReopenAwaiting,
            listOf(),
            "",
            Cache(),
            0,
            { Unit },
            Cache()
        )

        result.shouldBeEmpty()
    }
    "should filter issues where resolution is null and unresolved is in excluded resolution" {
        val moduleExecutor = getMockModuleExecutor(
            registry = moduleRegistryMock,
            searchIssues = { _, _, _, _, _ -> listOf(mockIssue(resolution = null)) }
        )

        val result = moduleExecutor.getIssues(
            Arisa.Modules.ReopenAwaiting,
            listOf(),
            "",
            Cache(),
            0,
            { Unit },
            Cache()
        )

        result.shouldBeEmpty()
    }

    "should execute each module" {
        val registryMock = mockk<ModuleRegistry>()
        val spy1 = spyk<(Issue, Instant) -> Pair<String, Either<ModuleError, ModuleResponse>>>()
        val spy2 = spyk<(Issue, Instant) -> Pair<String, Either<ModuleError, ModuleResponse>>>()
        every { spy1.invoke(any(), any()) } returns ("mock" to Either.left(OperationNotNeededModuleResponse))
        every { spy2.invoke(any(), any()) } returns ("mock2" to Either.left(OperationNotNeededModuleResponse))
        every { registryMock.getModules() } returns listOf(
            ModuleRegistry.Entry(
                "mock",
                Arisa.Modules.Attachment,
                DEFAULT_JQL,
                spy1
            ),
            ModuleRegistry.Entry(
                "mock2",
                Arisa.Modules.Attachment,
                DEFAULT_JQL,
                spy2
            )
        )
        val moduleExecutor = getMockModuleExecutor(registry = registryMock)

        val result = moduleExecutor.execute(Instant.now(), setOf("MC-1"))

        result.successful shouldBe true
        verify { spy1.invoke(any(), any()) }
        verify { spy2.invoke(any(), any()) }
    }
})

fun getMockModuleExecutor(
    registry: ModuleRegistry = moduleRegistryMock,
    queryCache: Cache<List<Issue>> = Cache(),
    issueUpdateContextCache: IssueUpdateContextCache = IssueUpdateContextCache(),
    searchIssues: (Cache<MutableSet<String>>, Cache<MutableSet<String>>, String, Int, () -> Unit) -> List<Issue> =
        { _, _, _, _, _ -> listOf(mockIssue()) }
): ModuleExecutor = ModuleExecutor(
    getConfig(),
    registry,
    queryCache,
    issueUpdateContextCache,
    searchIssues
)

private fun getConfig() = Config { addSpec(Arisa) }
    .from.json.watchFile("arisa.json")
    .from.map.flat(
        mapOf(
            "arisa.credentials.username" to "test",
            "arisa.credentials.password" to "test",
            "arisa.credentials.dandelionToken" to "test",
            "arisa.credentials.discordLogWebhook" to "test"
        )
    )
