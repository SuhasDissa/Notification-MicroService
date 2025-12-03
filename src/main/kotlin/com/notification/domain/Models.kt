package com.notification.domain

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
enum class NotificationType {
    EMAIL, SMS, PUSH
}

@Serializable
enum class NotificationStatus {
    QUEUED, PENDING, DELIVERED, FAILED
}

@Serializable
enum class Priority {
    HIGH, NORMAL, LOW
}

@Serializable
data class NotificationRequest(
    val type: NotificationType,
    val recipient: String,
    val template: String? = null,
    val subject: String? = null,
    val body: String? = null,
    val data: Map<String, String> = emptyMap(),
    val priority: Priority = Priority.NORMAL,
    val scheduledAt: String? = null
)

data class Notification(
    val id: UUID,
    val type: NotificationType,
    val recipient: String,
    val templateId: String?,
    val subject: String?,
    val body: String,
    val data: Map<String, String>,
    val status: NotificationStatus,
    val priority: Priority,
    val scheduledAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val attempts: Int,
    val lastAttemptAt: Instant?,
    val deliveredAt: Instant?,
    val errorMessage: String?,
    val provider: String?,
    val metadata: Map<String, String>
)

@Serializable
data class NotificationResponse(
    val notificationId: String,
    val status: NotificationStatus,
    val createdAt: String
)

@Serializable
data class NotificationStatusResponse(
    val notificationId: String,
    val type: NotificationType,
    val status: NotificationStatus,
    val attempts: Int,
    val lastAttemptAt: String?,
    val deliveredAt: String?
)

@Serializable
data class BatchNotificationRequest(
    val notifications: List<NotificationRequest>
)

data class Template(
    val id: String,
    val name: String,
    val type: NotificationType,
    val subject: String?,
    val body: String,
    val variables: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val active: Boolean
)

data class NotificationAudit(
    val id: Long,
    val notificationId: UUID,
    val eventType: String,
    val eventData: Map<String, String>,
    val createdAt: Instant
)

@Serializable
data class HealthCheckResponse(
    val status: String,
    val checks: Map<String, String>
)

data class NotificationEvent(
    val notification: Notification,
    val eventType: String,
    val timestamp: Instant
)
