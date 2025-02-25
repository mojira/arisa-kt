package io.github.mojira.arisa.registry

import com.uchuhimo.konf.Config
import io.github.mojira.arisa.ExecutionTimeframe
import io.github.mojira.arisa.domain.cloud.CloudIssue
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.modules.PrivateDuplicateModule
import io.github.mojira.arisa.modules.privacy.AccessTokenRedactor
import io.github.mojira.arisa.modules.privacy.PrivacyModule

/**
 * This class is the registry for modules that get executed immediately after a ticket has been updated.
 */
class InstantModuleRegistry(config: Config) : ModuleRegistry<CloudIssue>(config) {
    override fun getJql(timeframe: ExecutionTimeframe): String {
        return timeframe.getFreshlyUpdatedJql()
    }

    init {
        register(
            Arisa.Modules.PrivateDuplicate,
            PrivateDuplicateModule(
                config[Arisa.Modules.PrivateDuplicate.tag]
            )
        )

        register(
            Arisa.Modules.Privacy,
            PrivacyModule(
                config[Arisa.Modules.Privacy.message],
                config[Arisa.Modules.Privacy.commentNote],
                config[Arisa.Modules.Privacy.allowedEmailRegexes].map(String::toRegex),
                config[Arisa.Modules.Privacy.sensitiveTextRegexes].map(String::toRegex),
                AccessTokenRedactor,
                config[Arisa.Modules.Privacy.sensitiveFileNameRegexes].map(String::toRegex)
            )
        )
    }

    private fun <T> List<T>.toSetNoDuplicates(): Set<T> {
        val result = toMutableSet()
        if (result.size != size) {
            val duplicates = mutableSetOf<T>()
            for (element in this) {
                // If removal fails it is a duplicate element because it has already been removed
                if (!result.remove(element)) {
                    duplicates.add(element)
                }
            }
            throw IllegalArgumentException("Contains these duplicate elements: $duplicates")
        }

        return result
    }
}
