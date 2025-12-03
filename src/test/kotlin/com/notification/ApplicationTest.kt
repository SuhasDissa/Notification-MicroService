package com.notification

import com.notification.domain.NotificationRequest
import com.notification.domain.NotificationType
import com.notification.domain.Priority
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class ApplicationTest {

    @Test
    fun testHealthEndpoint() = testApplication {
        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("status"))
    }

    @Test
    fun testMetricsEndpoint() = testApplication {
        val response = client.get("/metrics")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testReadinessProbe() = testApplication {
        val response = client.get("/health/ready")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("ready"))
    }

    @Test
    fun testLivenessProbe() = testApplication {
        val response = client.get("/health/live")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("alive"))
    }
}
