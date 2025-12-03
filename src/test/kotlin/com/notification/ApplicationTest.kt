package com.notification

import com.notification.domain.NotificationRequest
import com.notification.domain.NotificationType
import com.notification.domain.Priority
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.*

class ApplicationTest {

    companion object {
        private val postgres = PostgreSQLContainer<Nothing>("postgres:15-alpine")

        @JvmStatic
        @BeforeAll
        fun setup() {
            postgres.start()
        }

        @JvmStatic
        @AfterAll
        fun teardown() {
            postgres.stop()
        }
    }

    private fun ApplicationTestBuilder.configureEnvironment() {
        environment {
            config = MapApplicationConfig().apply {
                put("database.url", postgres.jdbcUrl)
                put("database.user", postgres.username)
                put("database.password", postgres.password)
                put("database.driver", "org.postgresql.Driver")
            }
        }
    }

    @Test
    fun testHealthEndpoint() = testApplication {
        configureEnvironment()
        application {
            module()
        }
        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("status"))
    }

    @Test
    fun testMetricsEndpoint() = testApplication {
        configureEnvironment()
        application {
            module()
        }
        val response = client.get("/metrics")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testReadinessProbe() = testApplication {
        configureEnvironment()
        application {
            module()
        }
        val response = client.get("/health/ready")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("ready"))
    }

    @Test
    fun testLivenessProbe() = testApplication {
        configureEnvironment()
        application {
            module()
        }
        val response = client.get("/health/live")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("alive"))
    }
}
