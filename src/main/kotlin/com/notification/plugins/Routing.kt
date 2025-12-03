package com.notification.plugins

import com.notification.routes.healthRoutes
import com.notification.routes.notificationRoutes
import com.notification.service.NotificationService
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(notificationService: NotificationService) {
    routing {
        healthRoutes(notificationService)
        notificationRoutes(notificationService)
    }
}
