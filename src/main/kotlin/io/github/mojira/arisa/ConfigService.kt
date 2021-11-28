package io.github.mojira.arisa

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.Feature
import com.uchuhimo.konf.source.yaml
import io.github.mojira.arisa.infrastructure.config.Arisa

class ConfigService {
    val config: Config = Config { addSpec(Arisa) }
        // Only enable strict config parsing for YAML files; environment variables and system properties
        // likely contain entries completely unrelated to Arisa
        .from.enabled(Feature.FAIL_ON_UNKNOWN_PATH).yaml.watchFile("config/config.yml")
        .from.enabled(Feature.FAIL_ON_UNKNOWN_PATH).yaml.watchFile("config/local.yml")
        .from.env()
        .from.systemProperties()
        .validateRequired()
}
