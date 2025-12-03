package com.notification.service

import com.notification.config.NotificationConfig
import com.notification.config.ProvidersConfig
import com.notification.domain.*
import com.notification.provider.*
import com.notification.repository.NotificationRepository
import com.notification.repository.TemplateRepository
import com.notification.util.CircuitBreaker
import com.notification.util.RetryHandler
import com.notification.util.TemplateEngine
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.MDC
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val templateRepository: TemplateRepository,
    private val emailProvider: EmailProvider,
    private val smsProvider: SmsProvider,
    private val pushProvider: PushProvider,
    private val config: NotificationConfig
) {
    private val retryHandler = RetryHandler(config.maxRetries, config.retryDelays)
    private val circuitBreakers = mutableMapOf<String, CircuitBreaker>()

    suspend fun createNotification(request: NotificationRequest): NotificationResponse {
        val notificationId = UUID.randomUUID()

        // Set MDC for logging correlation
        MDC.put("notificationId", notificationId.toString())
        MDC.put("type", request.type.name)
        MDC.put("recipient", request.recipient)

        logger.info { "Creating notification for ${request.recipient}" }

        // Get template if specified
        val template = request.template?.let { templateRepository.findById(it) }

        // Render body and subject
        val body = when {
            template != null -> TemplateEngine.render(template, request.data)
            request.body != null -> request.body
            else -> throw IllegalArgumentException("Either template or body must be provided")
        }

        val subject = when {
            template != null -> TemplateEngine.renderSubject(template, request.data)
            else -> request.subject
        }

        val scheduledAt = request.scheduledAt?.let { Instant.parse(it) }

        val notification = Notification(
            id = notificationId,
            type = request.type,
            recipient = request.recipient,
            templateId = request.template,
            subject = subject,
            body = body,
            data = request.data,
            status = NotificationStatus.QUEUED,
            priority = request.priority,
            scheduledAt = scheduledAt,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            attempts = 0,
            lastAttemptAt = null,
            deliveredAt = null,
            errorMessage = null,
            provider = null,
            metadata = emptyMap()
        )

        val created = notificationRepository.create(notification)

        // Create audit log
        notificationRepository.createAuditLog(
            NotificationAudit(
                id = 0,
                notificationId = notificationId,
                eventType = "CREATED",
                eventData = mapOf("status" to "QUEUED"),
                createdAt = Instant.now()
            )
        )

        logger.info { "Notification created successfully with id: $notificationId" }

        MDC.clear()

        return NotificationResponse(
            notificationId = notificationId.toString(),
            status = created.status,
            createdAt = created.createdAt.toString()
        )
    }

    suspend fun getNotificationStatus(id: UUID): NotificationStatusResponse? {
        val notification = notificationRepository.findById(id) ?: return null

        return NotificationStatusResponse(
            notificationId = notification.id.toString(),
            type = notification.type,
            status = notification.status,
            attempts = notification.attempts,
            lastAttemptAt = notification.lastAttemptAt?.toString(),
            deliveredAt = notification.deliveredAt?.toString()
        )
    }

    suspend fun processNotification(notification: Notification) {
        MDC.put("notificationId", notification.id.toString())
        MDC.put("type", notification.type.name)

        logger.info { "Processing notification ${notification.id}" }

        val provider = getProvider(notification.type)
        val circuitBreaker = getCircuitBreaker(provider.getProviderName())

        try {
            val result = retryHandler.executeWithRetry("send-notification") { attempt ->
                val updatedNotification = notification.copy(
                    status = NotificationStatus.PENDING,
                    attempts = attempt + 1,
                    lastAttemptAt = Instant.now()
                )
                notificationRepository.update(updatedNotification)

                circuitBreaker.execute {
                    provider.send(updatedNotification)
                }
            }

            if (result.success) {
                val successNotification = notification.copy(
                    status = NotificationStatus.DELIVERED,
                    deliveredAt = Instant.now(),
                    provider = provider.getProviderName()
                )
                notificationRepository.update(successNotification)

                notificationRepository.createAuditLog(
                    NotificationAudit(
                        id = 0,
                        notificationId = notification.id,
                        eventType = "DELIVERED",
                        eventData = mapOf("provider" to provider.getProviderName()),
                        createdAt = Instant.now()
                    )
                )

                logger.info { "Notification ${notification.id} delivered successfully" }
            } else {
                handleFailure(notification, result.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to process notification ${notification.id}" }
            handleFailure(notification, e.message ?: "Exception occurred")
        } finally {
            MDC.clear()
        }
    }

    private suspend fun handleFailure(notification: Notification, errorMessage: String) {
        val failedNotification = notification.copy(
            status = NotificationStatus.FAILED,
            errorMessage = errorMessage
        )
        notificationRepository.update(failedNotification)

        notificationRepository.createAuditLog(
            NotificationAudit(
                id = 0,
                notificationId = notification.id,
                eventType = "FAILED",
                eventData = mapOf("error" to errorMessage),
                createdAt = Instant.now()
            )
        )

        logger.error { "Notification ${notification.id} failed: $errorMessage" }
    }

    private fun getProvider(type: NotificationType): NotificationProvider {
        return when (type) {
            NotificationType.EMAIL -> emailProvider
            NotificationType.SMS -> smsProvider
            NotificationType.PUSH -> pushProvider
        }
    }

    private fun getCircuitBreaker(providerName: String): CircuitBreaker {
        return circuitBreakers.getOrPut(providerName) {
            CircuitBreaker(config.circuitBreaker, providerName)
        }
    }

    suspend fun healthCheck(): Map<String, String> {
        return mapOf(
            "database" to "up",
            "emailProvider" to if (emailProvider.healthCheck()) "up" else "down",
            "smsProvider" to if (smsProvider.healthCheck()) "up" else "down",
            "pushProvider" to if (pushProvider.healthCheck()) "up" else "down"
        )
    }
}
