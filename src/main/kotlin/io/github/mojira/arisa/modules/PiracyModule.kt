package io.github.mojira.arisa.modules

import arrow.core.Either
import arrow.core.left

data class PiracyModuleRequest(
    val environment: String?,
    val summary: String?,
    val description: String?
)

class PiracyModule(
    val resolveAsInvalid: () -> Either<Throwable, Unit>,
    val addComment: (String) -> Either<Throwable, Unit>
) : Module<PiracyModuleRequest> {

    val piracySignatures = listOf(
        "Minecraft Launcher null",
        "Bootstrap 0",
        "Launcher: 1.0.10  (bootstrap 4)",
        "Launcher: 1.0.10  (bootstrap 5)",
        "Launcher 3.0.0",
        "Launcher: 3.1.0",
        "Launcher: 3.1.1",
        "Launcher: 3.1.4",
        "1.0.8",
        "uuid sessionId",
        "auth_access_token",
        "windows-\${arch}",
        "keicraft",
        "keinett",
        "nodus",
        "iridium",
        "mcdonalds",
        "uranium",
        "nova",
        "divinity",
        "gemini",
        "mineshafter",
        "Team-NeO",
        "DarkLBP",
        "Launcher X",
        "PHVL",
        "Pre-Launcher v6",
        "LauncherFEnix",
        "TLauncher"
    )

    override fun invoke(request: PiracyModuleRequest): Either<ModuleError, ModuleResponse> {
        if (request.description.isNullOrEmpty() && request.environment.isNullOrEmpty() && request.summary.isNullOrEmpty()) {
            return OperationNotNeededModuleResponse.left()
        }
        TODO()
    }
}
