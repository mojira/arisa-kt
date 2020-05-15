package io.kotest.provided

import io.kotest.core.config.AbstractProjectConfig

object ProjectConfig : AbstractProjectConfig() {
    override val parallelism = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
}
