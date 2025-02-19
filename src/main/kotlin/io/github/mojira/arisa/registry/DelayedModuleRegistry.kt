package io.github.mojira.arisa.registry

import com.uchuhimo.konf.Config
import io.github.mojira.arisa.ExecutionTimeframe
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.modules.DuplicateMessageModule
import java.time.Duration

/**
 * This class is the registry for modules that get executed `commentDelayMinutes` after the ticket has been updated.
 */
//class DelayedModuleRegistry(config: Config) : ModuleRegistry(config) {
//    private val delayOffset = Duration.ofMinutes(config[Arisa.Modules.DuplicateMessage.commentDelayMinutes])
//
//    override fun getJql(timeframe: ExecutionTimeframe): String {
//        return timeframe.getDelayedUpdatedJql(delayOffset)
//    }
//
//    init {
//        register(
//            Arisa.Modules.DuplicateMessage,
//            DuplicateMessageModule(
//                config[Arisa.Modules.DuplicateMessage.commentDelayMinutes],
//                config[Arisa.Modules.DuplicateMessage.message],
//                config[Arisa.Modules.DuplicateMessage.forwardMessage],
//                config[Arisa.Modules.DuplicateMessage.ticketMessages],
//                config[Arisa.Modules.DuplicateMessage.privateMessage],
//                config[Arisa.Modules.DuplicateMessage.preventMessageTags],
//                config[Arisa.Modules.DuplicateMessage.resolutionMessages]
//            )
//        )
//    }
//}
