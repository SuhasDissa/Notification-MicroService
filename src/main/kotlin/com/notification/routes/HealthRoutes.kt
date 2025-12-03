package com.notification.routes

import com.notification.domain.HealthCheckResponse
import com.notification.service.NotificationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.healthRoutes(notificationService: NotificationService) {
    get("/health") {
        val checks = notificationService.healthCheck()
        val status = if (checks.values.all { it == "up" }) "healthy" else "degraded"

        val response = HealthCheckResponse(
            status = status,
            checks = checks
        )

        call.respond(HttpStatusCode.OK, response)
    }

    get("/health/ready") {
        // Readiness probe
        call.respond(HttpStatusCode.OK, mapOf("status" to "ready"))
    }

    get("/health/live") {
        // Liveness probe
        call.respond(HttpStatusCode.OK, mapOf("status" to "alive"))
    }
}
