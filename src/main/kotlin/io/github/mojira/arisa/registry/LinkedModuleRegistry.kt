package io.github.mojira.arisa.registry

/**
 * This class is the registry for the UpdateLinkedModule.
 * It only updates the linked field once if it's not set, and otherwise only updates it at most once per day.
 * This is done in order to avoid spam.
 */
// class LinkedModuleRegistry(config: Config) : ModuleRegistry<Issue>(config) {
//    private val delayOffset = Duration.ofHours(config[Arisa.Modules.UpdateLinked.updateIntervalHours])
//
//    override fun getJql(timeframe: ExecutionTimeframe): String {
//        val freshlyUpdatedJql = timeframe.getFreshlyUpdatedJql()
//        val delayedJql = timeframe.getDelayedUpdatedJql(delayOffset)
//
//        return "($freshlyUpdatedJql) OR ($delayedJql)"
//    }
//
//    init {
//        register(
//            Arisa.Modules.UpdateLinked,
//            UpdateLinkedModule(config[Arisa.Modules.UpdateLinked.updateIntervalHours])
//        )
//    }
// }
