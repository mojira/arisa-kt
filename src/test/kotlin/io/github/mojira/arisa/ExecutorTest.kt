package io.github.mojira.arisa

import arrow.core.Either
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.github.mojira.arisa.domain.Issue
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.modules.FailedModuleResponse
import io.github.mojira.arisa.modules.OperationNotNeededModuleResponse
import io.github.mojira.arisa.registry.ModuleRegistry
import io.github.mojira.arisa.utils.mockIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant

val moduleRegistryMock = mockk<ModuleRegistry>()
val failedModuleRegistryMock = mockk<ModuleRegistry>()
val moduleExecutorMock = mockk<ModuleExecutor>()
val failedModuleExecutorMock = mockk<ModuleExecutor>()

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

    "should add failed tickets" {
        val executor = getMockExecutor(listOf(failedModuleRegistryMock))

        val result = executor.execute(Instant.now(), setOf("MC-1"))

        result.failedTickets shouldContain "MC-1"
        result.successful shouldBe true
    }

    "should not add succesful tickets" {
        val executor = getMockExecutor(listOf(moduleRegistryMock))

        val result = executor.execute(Instant.now(), setOf("MC-1"))

        result.failedTickets.shouldBeEmpty()
        result.successful shouldBe true
    }

    "should fail execution if search issues fails" {
        val executor =
            getMockExecutor(
                listOf(moduleRegistryMock),
                searchIssues = { _, _, _ -> throw RuntimeException() }
            )

        val result = executor.execute(Instant.now(), setOf("MC-1"))

        result.failedTickets.shouldBeEmpty()
        result.successful shouldBe false
    }
})

fun getMockExecutor(
    registries: List<ModuleRegistry>,
    searchIssues: (String, Int, () -> Unit) -> List<Issue> =
        { _, _, finishedCallback ->
            finishedCallback()
            listOf(mockIssue())
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
