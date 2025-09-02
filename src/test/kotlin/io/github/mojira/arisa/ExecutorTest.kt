package io.github.mojira.arisa

import arrow.core.Either
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.github.mojira.arisa.domain.cloud.CloudIssue
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import io.github.mojira.arisa.registry.ModuleRegistry
import io.github.mojira.arisa.utils.mockCloudIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant

private val moduleRegistryMock = mockk<ModuleRegistry<CloudIssue>>()
private val failedModuleRegistryMock = mockk<ModuleRegistry<CloudIssue>>()
private val moduleExecutorMock = mockk<ModuleExecutor>()
private val failedModuleExecutorMock = mockk<ModuleExecutor>()

private val dummyTimeframe = ExecutionTimeframe(Instant.now(), Instant.now(), mapOf(), true)

class ExecutorTest : StringSpec({
    every { moduleRegistryMock.getEnabledModules() } returns listOf(
        ModuleRegistry.Entry(
            "mock",
            Arisa.Modules.Attachment,
            { _, _ -> "mock" to Either.left(OperationNotNeededModuleResponse) },
            moduleExecutorMock
        )
    )
    every { failedModuleRegistryMock.getEnabledModules() } returns listOf(
        ModuleRegistry.Entry(
            "mock",
            Arisa.Modules.Attachment,
            { _, _ -> "mock" to Either.left(FailedModuleResponse()) },
            failedModuleExecutorMock
        )
    )
    every { moduleRegistryMock.getFullJql(any(), any()) } returns ""
    every { failedModuleRegistryMock.getFullJql(any(), any()) } returns ""
    every { moduleExecutorMock.executeModule(any(), any(), any()) } returns Unit

    val addFailed = slot<(String) -> Unit>()
    every { failedModuleExecutorMock.executeModule(any(), capture(addFailed), any()) } answers {
        addFailed.captured("MC-1")
    }

    "should not add tickets that failed before" {
        val executor = getMockExecutor(listOf(failedModuleRegistryMock))

        val result = executor.execute(dummyTimeframe, setOf("MC-1"))

        result.failedTickets.shouldBeEmpty()
        result.successful shouldBe true
    }

    "should add failed tickets" {
        val executor = getMockExecutor(
            listOf(failedModuleRegistryMock),
            searchIssues = { _, _, finishedCallback ->
                run {
                    finishedCallback()
                    null to listOf(mockCloudIssue("MC-1"))
                }
            }
        )

        val result = executor.execute(dummyTimeframe, emptySet())

        result.failedTickets shouldContain "MC-1"
        result.successful shouldBe true
    }

    "should not add succesful tickets" {
        val executor = getMockExecutor(listOf(moduleRegistryMock))

        val result = executor.execute(dummyTimeframe, setOf("MC-1"))

        result.failedTickets.shouldBeEmpty()
        result.successful shouldBe true
    }

    "should fail execution if search issues fails" {
        val executor =
            getMockExecutor(
                listOf(moduleRegistryMock),
                searchIssues = { _, _, _ -> throw RuntimeException() }
            )

        val result = executor.execute(dummyTimeframe, setOf("MC-1"))

        result.failedTickets.shouldBeEmpty()
        result.successful shouldBe false
    }
})

fun getMockExecutor(
    registries: List<ModuleRegistry<CloudIssue>>,
    searchIssues: (String, String?, () -> Unit) -> Pair<String?, List<CloudIssue>> =
        { _, _, finishedCallback ->
            finishedCallback()
            null to listOf(mockCloudIssue())
        }
): Executor = Executor(
    getConfig(),
    registries,
    searchIssues
)

private fun getConfig() = Config { addSpec(Arisa) }
    .from.yaml.watchFile("config/config.yml")
    .from.map.flat(
        mapOf(
            "arisa.credentials.username" to "test",
            "arisa.credentials.password" to "test",
            "arisa.credentials.dandelionToken" to "test",
            "arisa.credentials.discordLogWebhook" to "test"
        )
    )
