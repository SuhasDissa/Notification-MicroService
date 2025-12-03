package com.notification.routes

import com.notification.domain.*
import com.notification.service.NotificationService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

private val logger = KotlinLogging.logger {}

fun Route.notificationRoutes(notificationService: NotificationService) {
    route("/api/v1/notifications") {
        post {
            try {
                val request = call.receive<NotificationRequest>()
                logger.info { "Received notification request for ${request.recipient}" }

                val response = notificationService.createNotification(request)
                call.respond(HttpStatusCode.Created, response)
            } catch (e: IllegalArgumentException) {
                logger.warn(e) { "Invalid notification request" }
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to (e.message ?: "Invalid request"))
                )
            } catch (e: Exception) {
                logger.error(e) { "Error creating notification" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to create notification")
                )
            }
        }

        get("/{id}") {
            try {
                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing id parameter")
                )

                val uuid = try {
                    UUID.fromString(id)
                } catch (e: IllegalArgumentException) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid UUID format")
                    )
                }

                val status = notificationService.getNotificationStatus(uuid)
                if (status != null) {
                    call.respond(HttpStatusCode.OK, status)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Notification not found")
                    )
                }
            } catch (e: Exception) {
                logger.error(e) { "Error retrieving notification status" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to retrieve notification status")
                )
            }
        }

        post("/batch") {
            try {
                val batchRequest = call.receive<BatchNotificationRequest>()
                logger.info { "Received batch notification request with ${batchRequest.notifications.size} notifications" }

                val responses = batchRequest.notifications.map { request ->
                    try {
                        notificationService.createNotification(request)
                    } catch (e: Exception) {
                        logger.error(e) { "Error in batch notification" }
                        null
                    }
                }

                call.respond(HttpStatusCode.Created, mapOf("results" to responses))
            } catch (e: Exception) {
                logger.error(e) { "Error processing batch notifications" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to process batch notifications")
                )
            }
        }
    }
}
