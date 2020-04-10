package io.github.mojira.arisa

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.github.mojira.arisa.infrastructure.Cache
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.connectToJira
import org.slf4j.LoggerFactory
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

    val moduleExecutor = ModuleExecutor(jiraClient, config, Cache())
    while (true) {
        moduleExecutor.execute()

        TimeUnit.SECONDS.sleep(config[Arisa.Issues.checkInterval])
    }
}
