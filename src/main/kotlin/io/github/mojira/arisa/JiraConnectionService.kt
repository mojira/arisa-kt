package io.github.mojira.arisa

import com.uchuhimo.konf.Config
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.jira.IncorrectlyCapitalizedUsernameException
import io.github.mojira.arisa.infrastructure.jira.connectToJira
import java.lang.Long.max
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

class JiraConnectionService(
    private val config: Config
) {
    companion object {
        const val MIN_TIME_BETWEEN_EXECUTION_CYCLES_IN_SECONDS = 10L

        /**
         * How long ago the last successful connection needs to be for Arisa to try to relog
         */
        private const val MAX_SECONDS_SINCE_LAST_SUCCESSFUL_CONNECTION = 60L

        /**
         * How long Arisa should wait after it was unable to relog
         */
        private const val WAIT_TIME_AFTER_UNSUCCESSFUL_RELOG_IN_SECONDS = 5 * 60L // 5 minutes

        /**
         * How long Arisa should wait after a connection error before continuing without trying to relog
         */
        private const val WAIT_TIME_AFTER_CONNECTION_ERROR_IN_SECONDS = 40L
    }

    private var lastSuccessfulConnection = Instant.now()

    /**
     * Tries to establish a connection and log into Jira.
     * @returns null if able to connect, the connection exception otherwise
     */
    @Suppress("TooGenericExceptionCaught")
    private fun establishConnection(): Exception? {
        return try {
            val client = connectToJira(
                config[Arisa.Credentials.username],
                config[Arisa.Credentials.password],
                config[Arisa.Issues.url]
            )

            log.info("Successfully connected to jira")
            lastSuccessfulConnection = Instant.now()
            jiraClient = client

            null
        } catch (exception: Exception) {
            exception
        }
    }

    /**
     * Try relogging if the last successful connection was at least [MAX_SECONDS_SINCE_LAST_SUCCESSFUL_CONNECTION] ago.
     * @return
     * - [RelogResult.SuccessfulRelog] if relog was successful
     * - [RelogResult.UnsucessfulRelog] if unable to relog
     * - [RelogResult.NoRelogAttempted] if no relog was attempted because the last relog happened recently
     */
    fun tryRelog(): RelogResult {
        val secondsSinceLastSuccessfulConnection = Duration.between(lastSuccessfulConnection, Instant.now()).toSeconds()

        return if (secondsSinceLastSuccessfulConnection > MAX_SECONDS_SINCE_LAST_SUCCESSFUL_CONNECTION) {
            log.info("Trying to relog")

            val exception = establishConnection() ?: return RelogResult.SuccessfulRelog()

            val relogResult = RelogResult.UnsucessfulRelog()
            log.error(
                "Could not reconnect. Will attempt to relog again in ${ relogResult.sleepTimeInSeconds } seconds."
            )
            log.debug("Connection error:", exception)

            relogResult
        } else {
            val relogResult = RelogResult.NoRelogAttempted()
            log.info(
                "Something went wrong. " +
                    "Will wait for ${ relogResult.sleepTimeInSeconds } seconds and see if it'll work then."
            )
            relogResult
        }
    }

    /**
     * Tries to reconnect after the connection failed
     * Only used in [connect] below when initially connecting to Jira.
     */
    private fun reconnect() {
        var relogResult = tryRelog()
        while (relogResult !is RelogResult.SuccessfulRelog) {
            val secondsToSleep = relogResult.sleepTimeInSeconds
            TimeUnit.SECONDS.sleep(secondsToSleep)
            relogResult = tryRelog()
        }
    }

    /**
     * Connect to Jira for the first time
     * Tries to reconnect until it has established a connection
     */
    fun connect() {
        val exception = establishConnection() ?: return

        // Rethrow; startup should fail and Arisa config should be fixed
        if (exception is IncorrectlyCapitalizedUsernameException) {
            throw exception
        } else {
            log.error("Could not connect to Jira", exception)
            reconnect()
        }
    }

    /**
     * Notifies the connection service that a successful connection took place.
     * Resets the [lastSuccessfulConnection] timer.
     */
    fun notifyOfSuccessfulConnection() {
        lastSuccessfulConnection = Instant.now()
    }

    /**
     * Stores information about a relog attempt, and how long to wait after a specific relog outcome
     */
    interface RelogResult {
        class SuccessfulRelog : RelogResult {
            override val sleepTimeInSeconds = MIN_TIME_BETWEEN_EXECUTION_CYCLES_IN_SECONDS
        }

        class UnsucessfulRelog : RelogResult {
            override val sleepTimeInSeconds =
                max(WAIT_TIME_AFTER_UNSUCCESSFUL_RELOG_IN_SECONDS, MIN_TIME_BETWEEN_EXECUTION_CYCLES_IN_SECONDS)
        }

        class NoRelogAttempted : RelogResult {
            override val sleepTimeInSeconds =
                max(WAIT_TIME_AFTER_CONNECTION_ERROR_IN_SECONDS, MIN_TIME_BETWEEN_EXECUTION_CYCLES_IN_SECONDS)
        }

        val sleepTimeInSeconds: Long
    }
}
