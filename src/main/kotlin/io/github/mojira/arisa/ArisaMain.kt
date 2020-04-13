package io.github.mojira.arisa

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.github.mojira.arisa.infrastructure.Cache
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.connectToJira
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

val log = LoggerFactory.getLogger("Arisa")

fun main() {
    val config = Config { addSpec(Arisa) }
        .from.yaml.watchFile("arisa.yml")
        .from.json.watchFile("arisa.json")
        .from.env()
        .from.systemProperties()

    val jiraClient =
        connectToJira(
            config[Arisa.Credentials.username],
            config[Arisa.Credentials.password],
            config[Arisa.Issues.url]
        )

    log.info("Connected to jira")

    val lastRunFile = File("last-run")
    val lastRun =
        (if (lastRunFile.exists())
            lastRunFile.readText()
        else "")
            .split(",")

    var lastRunTime =
        if (lastRun[0].isNotEmpty())
            lastRun[0].toLong()
        else Instant.now().minus(5, ChronoUnit.MINUTES).toEpochMilli()

    val failedTickets = lastRun.subList(1, lastRun.size).toSet()

    val cache = Cache(failedTickets)
    val moduleExecutor = ModuleExecutor(jiraClient, config, cache)
    while (true) {
        // save time before run, so nothing happening during the run is missed
        val curRunTime = Instant.now().toEpochMilli()

        moduleExecutor.execute(lastRunTime)
        val failed = cache.getFailedTickets().joinToString{ ",$it" } // even first entry should start with a comma

        lastRunFile.writeText("$curRunTime$failed")
        lastRunTime = curRunTime

        TimeUnit.SECONDS.sleep(config[Arisa.Issues.checkInterval])
    }
}
