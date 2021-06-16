package io.github.mojira.arisa

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.github.mojira.arisa.infrastructure.config.Arisa

class ConfigService {
    val config: Config = Config { addSpec(Arisa) }
        .from.yaml.watchFile("config/config.yml")
        .from.yaml.watchFile("config/local.yml")
        .from.env()
        .from.systemProperties()
}
