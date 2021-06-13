package io.github.mojira.arisa

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ExecutionTimeframe(
    val lastRunTime: Instant,
    val currentRunTime: Instant,
    private val openEnded: Boolean
) {
    companion object {
        private const val MAX_TIMEFRAME_DURATION_IN_MINUTES = 10L

        /**
         * @return An [ExecutionTimeframe] beginning at [LastRun], either until right now,
         * or until [MAX_TIMEFRAME_DURATION_IN_MINUTES] after [LastRun].
         */
        fun getTimeframeFromLastRun(): ExecutionTimeframe {
            // Save time before run, so nothing happening during the run is missed
            val currentTime = Instant.now()
            val endOfMaxTimeframe = LastRun.time.plus(MAX_TIMEFRAME_DURATION_IN_MINUTES, ChronoUnit.MINUTES)

            // The time at which we execute the bot.
            // If we exceed our maximum time frame, act as if we were executing at the end of the maximum time frame.
            // This is to make sure that `last-run` is updated once in a while,
            // even if the bot is catching up after a long downtime.
            val runOpenEnded = currentTime.isBefore(endOfMaxTimeframe)
            val currentRunTime = if (runOpenEnded) {
                currentTime
            } else {
                endOfMaxTimeframe
            }

            return ExecutionTimeframe(LastRun.time, currentRunTime, runOpenEnded)
        }
    }

    fun duration(): Duration = Duration.between(lastRunTime, currentRunTime).abs()

    /**
     * Adds a cap to a JQL query if this time frame is not open.
     *
     * @return If open ended: empty string. Otherwise: ` AND updated <= [currentRunTime]`
     */
    fun capIfNotOpenEnded(): String =
        if (openEnded) "" else " AND updated <= ${ currentRunTime.toEpochMilli() }"

    override fun toString(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        val start = formatter.format(LocalDateTime.ofInstant(lastRunTime, ZoneOffset.UTC))
        val end = if (openEnded) "NOW" else formatter.format(LocalDateTime.ofInstant(currentRunTime, ZoneOffset.UTC))

        return "[$start - $end]"
    }
}
