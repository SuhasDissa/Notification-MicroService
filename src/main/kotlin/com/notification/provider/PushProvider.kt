package com.notification.provider

import com.notification.config.PushProviderConfig
import com.notification.domain.Notification
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class PushProvider(private val config: PushProviderConfig) : NotificationProvider {

    override suspend fun send(notification: Notification): ProviderResult {
        return try {
            if (!config.enabled) {
                logger.warn { "Push notification provider is disabled" }
                return ProviderResult(
                    success = false,
                    message = "Push provider is disabled",
                    errorCode = "PROVIDER_DISABLED"
                )
            }

            // Mock push notification (in production, integrate with FCM/APNS)
            logger.info { "Push notification sent to ${notification.recipient}: ${notification.body}" }

            ProviderResult(success = true, message = "Push notification sent successfully (mock)")
        } catch (e: Exception) {
            logger.error(e) { "Failed to send push notification to ${notification.recipient}" }
            ProviderResult(
                success = false,
                message = e.message ?: "Failed to send push notification",
                errorCode = "PUSH_ERROR"
            )
        }
    }

    override suspend fun healthCheck(): Boolean {
        return config.enabled
    }

    override fun getProviderName(): String = "push-fcm"
}
