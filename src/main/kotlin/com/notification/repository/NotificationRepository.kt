package com.notification.repository

import com.notification.config.DatabaseFactory.dbQuery
import com.notification.domain.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.dao.id.EntityID
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

class NotificationRepository {

    suspend fun create(notification: Notification): Notification = dbQuery {
        NotificationsTable.insert {
            it[id] = EntityID(notification.id, NotificationsTable)
            it[type] = notification.type.name
            it[recipient] = notification.recipient
            it[templateId] = notification.templateId
            it[subject] = notification.subject
            it[body] = notification.body
            it[data] = Json.encodeToString(notification.data)
            it[status] = notification.status.name
            it[priority] = notification.priority.name
            it[scheduledAt] = notification.scheduledAt
            it[createdAt] = notification.createdAt
            it[updatedAt] = notification.updatedAt
            it[attempts] = notification.attempts
            it[lastAttemptAt] = notification.lastAttemptAt
            it[deliveredAt] = notification.deliveredAt
            it[errorMessage] = notification.errorMessage
            it[provider] = notification.provider
            it[metadata] = Json.encodeToString(notification.metadata)
        }
        logger.info { "Notification created with id: ${notification.id}" }
        notification
    }

    suspend fun findById(id: UUID): Notification? = dbQuery {
        NotificationsTable.select { NotificationsTable.id eq EntityID(id, NotificationsTable) }
            .map { rowToNotification(it) }
            .singleOrNull()
    }

    suspend fun update(notification: Notification): Notification = dbQuery {
        NotificationsTable.update({ NotificationsTable.id eq EntityID(notification.id, NotificationsTable) }) {
            it[status] = notification.status.name
            it[updatedAt] = Instant.now()
            it[attempts] = notification.attempts
            it[lastAttemptAt] = notification.lastAttemptAt
            it[deliveredAt] = notification.deliveredAt
            it[errorMessage] = notification.errorMessage
            it[provider] = notification.provider
        }
        logger.info { "Notification updated: ${notification.id}" }
        notification
    }

    suspend fun findPendingNotifications(limit: Int = 100): List<Notification> = dbQuery {
        NotificationsTable.select {
                (NotificationsTable.status eq NotificationStatus.QUEUED.name) or
                        (NotificationsTable.status eq NotificationStatus.PENDING.name)
            }
            .limit(limit)
            .map { rowToNotification(it) }
    }

    suspend fun findScheduledNotifications(now: Instant): List<Notification> = dbQuery {
        NotificationsTable.select {
                (NotificationsTable.status eq NotificationStatus.QUEUED.name) and
                        (NotificationsTable.scheduledAt lessEq now)
            }
            .map { rowToNotification(it) }
    }

    suspend fun createAuditLog(audit: NotificationAudit) = dbQuery {
        NotificationAuditTable.insert {
            it[notificationId] = audit.notificationId
            it[eventType] = audit.eventType
            it[eventData] = Json.encodeToString(audit.eventData)
            it[createdAt] = audit.createdAt
        }
        logger.debug { "Audit log created for notification: ${audit.notificationId}" }
    }

    private fun rowToNotification(row: ResultRow): Notification {
        return Notification(
            id = row[NotificationsTable.id].value,
            type = NotificationType.valueOf(row[NotificationsTable.type]),
            recipient = row[NotificationsTable.recipient],
            templateId = row[NotificationsTable.templateId],
            subject = row[NotificationsTable.subject],
            body = row[NotificationsTable.body],
            data = try {
                Json.decodeFromString(row[NotificationsTable.data])
            } catch (e: Exception) {
                emptyMap()
            },
            status = NotificationStatus.valueOf(row[NotificationsTable.status]),
            priority = Priority.valueOf(row[NotificationsTable.priority]),
            scheduledAt = row[NotificationsTable.scheduledAt],
            createdAt = row[NotificationsTable.createdAt],
            updatedAt = row[NotificationsTable.updatedAt],
            attempts = row[NotificationsTable.attempts],
            lastAttemptAt = row[NotificationsTable.lastAttemptAt],
            deliveredAt = row[NotificationsTable.deliveredAt],
            errorMessage = row[NotificationsTable.errorMessage],
            provider = row[NotificationsTable.provider],
            metadata = try {
                Json.decodeFromString(row[NotificationsTable.metadata])
            } catch (e: Exception) {
                emptyMap()
            }
        )
    }
}
