package com.notification.config

data class DatabaseConfig(
    val url: String,
    val driver: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int,
    val minIdle: Int,
    val connectionTimeout: Long
)

data class KafkaConfig(
    val bootstrapServers: String,
    val groupId: String,
    val topic: String,
    val enableAutoCommit: Boolean
)

data class EmailProviderConfig(
    val type: String,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val from: String,
    val enabled: Boolean
)

data class SmsProviderConfig(
    val type: String,
    val accountSid: String,
    val authToken: String,
    val fromNumber: String,
    val enabled: Boolean
)

data class PushProviderConfig(
    val type: String,
    val serverKey: String,
    val enabled: Boolean
)

data class ProvidersConfig(
    val email: EmailProviderConfig,
    val sms: SmsProviderConfig,
    val push: PushProviderConfig
)

data class CircuitBreakerConfig(
    val failureThreshold: Int,
    val successThreshold: Int,
    val timeout: Long
)

data class NotificationConfig(
    val maxRetries: Int,
    val retryDelays: List<Long>,
    val circuitBreaker: CircuitBreakerConfig
)

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val validityMs: Long
)

data class SecurityConfig(
    val jwt: JwtConfig
)

data class MonitoringConfig(
    val prometheus: PrometheusConfig
)

data class PrometheusConfig(
    val enabled: Boolean
)

data class AppConfig(
    val database: DatabaseConfig,
    val kafka: KafkaConfig,
    val providers: ProvidersConfig,
    val notification: NotificationConfig,
    val security: SecurityConfig,
    val monitoring: MonitoringConfig
)
