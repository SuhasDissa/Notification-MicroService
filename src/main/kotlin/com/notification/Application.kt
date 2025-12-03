package com.notification

import com.notification.config.*
import com.notification.consumer.NotificationConsumer
import com.notification.plugins.*
import com.notification.provider.EmailProvider
import com.notification.provider.PushProvider
import com.notification.provider.SmsProvider
import com.notification.repository.NotificationRepository
import com.notification.repository.TemplateRepository
import com.notification.service.NotificationService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val logger = KotlinLogging.logger {}

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    logger.info { "Starting Notification Service..." }

    // Load configuration
    val appConfig = loadConfiguration()

    // Initialize database
    DatabaseFactory.init(appConfig.database)

    // Initialize repositories
    val notificationRepository = NotificationRepository()
    val templateRepository = TemplateRepository()

    // Initialize providers
    val emailProvider = EmailProvider(appConfig.providers.email)
    val smsProvider = SmsProvider(appConfig.providers.sms)
    val pushProvider = PushProvider(appConfig.providers.push)

    // Initialize service
    val notificationService = NotificationService(
        notificationRepository = notificationRepository,
        templateRepository = templateRepository,
        emailProvider = emailProvider,
        smsProvider = smsProvider,
        pushProvider = pushProvider,
        config = appConfig.notification
    )

    // Configure plugins
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureSecurity(appConfig.security)
    configureRouting(notificationService)

    // Start Kafka consumer (optional, can be disabled if Kafka is not available)
    try {
        val consumerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val notificationConsumer = NotificationConsumer(
            kafkaConfig = appConfig.kafka,
            notificationService = notificationService,
            notificationRepository = notificationRepository
        )

        environment.monitor.subscribe(ApplicationStarted) {
            logger.info { "Application started, initializing Kafka consumer..." }
            try {
                notificationConsumer.start(consumerScope)
            } catch (e: Exception) {
                logger.warn(e) { "Kafka consumer could not be started, continuing without it" }
            }
        }

        environment.monitor.subscribe(ApplicationStopping) {
            logger.info { "Application stopping, shutting down Kafka consumer..." }
            notificationConsumer.stop()
        }
    } catch (e: Exception) {
        logger.warn(e) { "Kafka consumer initialization failed, service will continue without it" }
    }

    logger.info { "Notification Service started successfully on port 8080" }
}

private fun Application.loadConfiguration(): AppConfig {
    val config = environment.config

    return AppConfig(
        database = DatabaseConfig(
            url = config.propertyOrNull("database.url")?.getString()
                ?: "jdbc:postgresql://localhost:5432/notifications",
            driver = config.propertyOrNull("database.driver")?.getString()
                ?: "org.postgresql.Driver",
            user = config.propertyOrNull("database.user")?.getString() ?: "notif_user",
            password = config.propertyOrNull("database.password")?.getString() ?: "notif_pass",
            maxPoolSize = config.propertyOrNull("database.maxPoolSize")?.getString()?.toInt() ?: 10,
            minIdle = config.propertyOrNull("database.minIdle")?.getString()?.toInt() ?: 2,
            connectionTimeout = config.propertyOrNull("database.connectionTimeout")?.getString()?.toLong()
                ?: 30000
        ),
        kafka = KafkaConfig(
            bootstrapServers = config.propertyOrNull("kafka.bootstrapServers")?.getString()
                ?: "localhost:9092",
            groupId = config.propertyOrNull("kafka.groupId")?.getString() ?: "notification-service",
            topic = config.propertyOrNull("kafka.topic")?.getString() ?: "notifications",
            enableAutoCommit = config.propertyOrNull("kafka.enableAutoCommit")?.getString()?.toBoolean()
                ?: false
        ),
        providers = ProvidersConfig(
            email = EmailProviderConfig(
                type = config.propertyOrNull("providers.email.type")?.getString() ?: "smtp",
                host = config.propertyOrNull("providers.email.host")?.getString() ?: "smtp.gmail.com",
                port = config.propertyOrNull("providers.email.port")?.getString()?.toInt() ?: 587,
                username = config.propertyOrNull("providers.email.username")?.getString() ?: "",
                password = config.propertyOrNull("providers.email.password")?.getString() ?: "",
                from = config.propertyOrNull("providers.email.from")?.getString()
                    ?: "noreply@notification-service.com",
                enabled = config.propertyOrNull("providers.email.enabled")?.getString()?.toBoolean()
                    ?: false
            ),
            sms = SmsProviderConfig(
                type = config.propertyOrNull("providers.sms.type")?.getString() ?: "twilio",
                accountSid = config.propertyOrNull("providers.sms.accountSid")?.getString() ?: "",
                authToken = config.propertyOrNull("providers.sms.authToken")?.getString() ?: "",
                fromNumber = config.propertyOrNull("providers.sms.fromNumber")?.getString() ?: "",
                enabled = config.propertyOrNull("providers.sms.enabled")?.getString()?.toBoolean() ?: false
            ),
            push = PushProviderConfig(
                type = config.propertyOrNull("providers.push.type")?.getString() ?: "fcm",
                serverKey = config.propertyOrNull("providers.push.serverKey")?.getString() ?: "",
                enabled = config.propertyOrNull("providers.push.enabled")?.getString()?.toBoolean() ?: false
            )
        ),
        notification = NotificationConfig(
            maxRetries = config.propertyOrNull("notification.maxRetries")?.getString()?.toInt() ?: 3,
            retryDelays = config.propertyOrNull("notification.retryDelays")?.getList()
                ?.map { it.toLong() } ?: listOf(1000, 5000, 15000),
            circuitBreaker = CircuitBreakerConfig(
                failureThreshold = config.propertyOrNull("notification.circuitBreaker.failureThreshold")
                    ?.getString()?.toInt() ?: 5,
                successThreshold = config.propertyOrNull("notification.circuitBreaker.successThreshold")
                    ?.getString()?.toInt() ?: 2,
                timeout = config.propertyOrNull("notification.circuitBreaker.timeout")?.getString()?.toLong()
                    ?: 30000
            )
        ),
        security = SecurityConfig(
            jwt = JwtConfig(
                secret = config.propertyOrNull("security.jwt.secret")?.getString()
                    ?: "change-this-secret-in-production",
                issuer = config.propertyOrNull("security.jwt.issuer")?.getString()
                    ?: "notification-service",
                audience = config.propertyOrNull("security.jwt.audience")?.getString()
                    ?: "notification-clients",
                realm = config.propertyOrNull("security.jwt.realm")?.getString()
                    ?: "Notification Service",
                validityMs = config.propertyOrNull("security.jwt.validityMs")?.getString()?.toLong()
                    ?: 3600000
            )
        ),
        monitoring = MonitoringConfig(
            prometheus = PrometheusConfig(
                enabled = config.propertyOrNull("monitoring.prometheus.enabled")?.getString()?.toBoolean()
                    ?: true
            )
        )
    )
}
