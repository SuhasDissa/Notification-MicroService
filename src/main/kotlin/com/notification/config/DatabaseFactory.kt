package com.notification.config

import com.notification.repository.NotificationAuditTable
import com.notification.repository.NotificationsTable
import com.notification.repository.TemplatesTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

private val logger = KotlinLogging.logger {}

object DatabaseFactory {
    fun init(config: DatabaseConfig) {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.url
            driverClassName = config.driver
            username = config.user
            password = config.password
            maximumPoolSize = config.maxPoolSize
            minimumIdle = config.minIdle
            connectionTimeout = config.connectionTimeout

            // Additional optimizations
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            validate()
        }

        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)

        logger.info { "Database connection established" }

        // Create tables if they don't exist
        transaction {
            SchemaUtils.create(NotificationsTable, TemplatesTable, NotificationAuditTable)

            // Create indexes
            exec("""
                CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications(status);
            """.trimIndent())

            exec("""
                CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);
            """.trimIndent())

            exec("""
                CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient);
            """.trimIndent())

            exec("""
                CREATE INDEX IF NOT EXISTS idx_notifications_scheduled_at ON notifications(scheduled_at);
            """.trimIndent())
        }

        logger.info { "Database schema initialized" }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
