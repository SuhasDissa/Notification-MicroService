package com.notification.provider

import com.notification.config.SmsProviderConfig
import com.notification.domain.Notification
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class SmsProvider(private val config: SmsProviderConfig) : NotificationProvider {

    override suspend fun send(notification: Notification): ProviderResult {
        return try {
            if (!config.enabled) {
                logger.warn { "SMS provider is disabled" }
                return ProviderResult(
                    success = false,
                    message = "SMS provider is disabled",
                    errorCode = "PROVIDER_DISABLED"
                )
            }

            // Mock SMS sending (in production, integrate with Twilio/AWS SNS)
            logger.info { "SMS sent to ${notification.recipient}: ${notification.body}" }

            ProviderResult(success = true, message = "SMS sent successfully (mock)")
        } catch (e: Exception) {
            logger.error(e) { "Failed to send SMS to ${notification.recipient}" }
            ProviderResult(
                success = false,
                message = e.message ?: "Failed to send SMS",
                errorCode = "SMS_ERROR"
            )
        }
    }

    override suspend fun healthCheck(): Boolean {
        return config.enabled
    }

    override fun getProviderName(): String = "sms-twilio"
}
