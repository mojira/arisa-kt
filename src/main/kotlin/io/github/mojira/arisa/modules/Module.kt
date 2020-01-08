package io.github.mojira.arisa.modules

import com.uchuhimo.konf.Config
import net.rcarz.jiraclient.JiraClient

abstract class Module<REQUEST>(val jiraClient: JiraClient, val config: Config) {
    abstract operator fun invoke(request: REQUEST): ModuleResponse
}

sealed class ModuleResponse
object SucessfulModuleResponse: ModuleResponse()
object OperationNotNeededModuleResponse: ModuleResponse()
data class FailedModuleResponse(val exceptions: List<Exception>): ModuleResponse()