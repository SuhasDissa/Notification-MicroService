package com.notification.repository

import com.notification.domain.NotificationStatus
import com.notification.domain.NotificationType
import com.notification.domain.Priority
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object NotificationsTable : UUIDTable("notifications") {
    val type = varchar("type", 50)
    val recipient = varchar("recipient", 255)
    val templateId = varchar("template_id", 100).nullable()
    val subject = varchar("subject", 500).nullable()
    val body = text("body")
    val data = text("data") // JSON stored as text
    val status = varchar("status", 50)
    val priority = varchar("priority", 20).default("NORMAL")
    val scheduledAt = timestamp("scheduled_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val attempts = integer("attempts").default(0)
    val lastAttemptAt = timestamp("last_attempt_at").nullable()
    val deliveredAt = timestamp("delivered_at").nullable()
    val errorMessage = text("error_message").nullable()
    val provider = varchar("provider", 100).nullable()
    val metadata = text("metadata") // JSON stored as text
}

object TemplatesTable : org.jetbrains.exposed.sql.Table("templates") {
    val id = varchar("id", 100)
    val name = varchar("name", 255)
    val type = varchar("type", 50)
    val subject = varchar("subject", 500).nullable()
    val body = text("body")
    val variables = text("variables") // JSON array stored as text
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val active = bool("active").default(true)

    override val primaryKey = PrimaryKey(id)
}

object NotificationAuditTable : LongIdTable("notification_audit") {
    val notificationId = uuid("notification_id").references(NotificationsTable.id)
    val eventType = varchar("event_type", 50)
    val eventData = text("event_data") // JSON stored as text
    val createdAt = timestamp("created_at")
}
