package io.github.mojira.arisa.modules.commands

class ShadowbanCommand {
    companion object {
        // This function gets set at initialization of the `LastRun` class.
        // Horrible hack but threading this through to the very top where `lastRun` is accessible would be difficult.
        var addShadowbannedUser: (String) -> Unit = { }
    }

    operator fun invoke(
        userName: String
    ): Int {
        addShadowbannedUser(userName)
        return 1
    }
}
