package io.github.mojira.arisa

import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.LoggerContext
import com.github.napstr.logback.DiscordAppender
import com.uchuhimo.konf.Config
import io.github.mojira.arisa.infrastructure.config.Arisa
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class WebhookService(
    private val config: Config
) {
    /**
     * Configures the logger, so that it sends log messages to the Discord webhooks
     */
    fun setLoggerWebhooks() {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext

        val discordAsync = context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("ASYNC_DISCORD") as AsyncAppender?
        if (discordAsync != null) {
            val discordAppender = discordAsync.getAppender("DISCORD") as DiscordAppender
            discordAppender.webhookUri = config[Arisa.Credentials.discordLogWebhook]
        }

        val discordErrorAsync =
            context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("ASYNC_ERROR_DISCORD") as AsyncAppender?
        if (discordErrorAsync != null) {
            val discordErrorAppender = discordErrorAsync.getAppender("ERROR_DISCORD") as DiscordAppender
            discordErrorAppender.webhookUri = config[Arisa.Credentials.discordErrorLogWebhook]
        }
    }
}
