package com.notification.repository

import com.notification.config.DatabaseFactory.dbQuery
import com.notification.domain.NotificationType
import com.notification.domain.Template
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import java.time.Instant

class TemplateRepository {

    suspend fun findById(id: String): Template? = dbQuery {
        TemplatesTable.select { (TemplatesTable.id eq id) and (TemplatesTable.active eq true) }
            .map { rowToTemplate(it) }
            .singleOrNull()
    }

    suspend fun findByType(type: NotificationType): List<Template> = dbQuery {
        TemplatesTable.select { (TemplatesTable.type eq type.name) and (TemplatesTable.active eq true) }
            .map { rowToTemplate(it) }
    }

    suspend fun create(template: Template): Template = dbQuery {
        TemplatesTable.insert {
            it[id] = template.id
            it[name] = template.name
            it[type] = template.type.name
            it[subject] = template.subject
            it[body] = template.body
            it[variables] = Json.encodeToString(template.variables)
            it[createdAt] = template.createdAt
            it[updatedAt] = template.updatedAt
            it[active] = template.active
        }
        template
    }

    private fun rowToTemplate(row: ResultRow): Template {
        return Template(
            id = row[TemplatesTable.id],
            name = row[TemplatesTable.name],
            type = NotificationType.valueOf(row[TemplatesTable.type]),
            subject = row[TemplatesTable.subject],
            body = row[TemplatesTable.body],
            variables = try {
                Json.decodeFromString(row[TemplatesTable.variables])
            } catch (e: Exception) {
                emptyList()
            },
            createdAt = row[TemplatesTable.createdAt],
            updatedAt = row[TemplatesTable.updatedAt],
            active = row[TemplatesTable.active]
        )
    }
}
