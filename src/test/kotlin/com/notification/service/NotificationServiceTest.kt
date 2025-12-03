package com.notification.service

import com.notification.config.CircuitBreakerConfig
import com.notification.config.NotificationConfig
import com.notification.domain.*
import com.notification.provider.EmailProvider
import com.notification.provider.ProviderResult
import com.notification.provider.PushProvider
import com.notification.provider.SmsProvider
import com.notification.repository.NotificationRepository
import com.notification.repository.TemplateRepository
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NotificationServiceTest {

    private lateinit var notificationRepository: NotificationRepository
    private lateinit var templateRepository: TemplateRepository
    private lateinit var emailProvider: EmailProvider
    private lateinit var smsProvider: SmsProvider
    private lateinit var pushProvider: PushProvider
    private lateinit var notificationService: NotificationService

    private val config = NotificationConfig(
        maxRetries = 3,
        retryDelays = listOf(1000, 5000, 15000),
        circuitBreaker = CircuitBreakerConfig(
            failureThreshold = 5,
            successThreshold = 2,
            timeout = 30000
        )
    )

    @BeforeEach
    fun setup() {
        notificationRepository = mockk()
        templateRepository = mockk()
        emailProvider = mockk()
        smsProvider = mockk()
        pushProvider = mockk()

        notificationService = NotificationService(
            notificationRepository,
            templateRepository,
            emailProvider,
            smsProvider,
            pushProvider,
            config
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `createNotification should create email notification successfully`() = runBlocking {
        // Given
        val request = NotificationRequest(
            type = NotificationType.EMAIL,
            recipient = "test@example.com",
            subject = "Test Subject",
            body = "Test Body",
            priority = Priority.NORMAL
        )

        val notification = Notification(
            id = UUID.randomUUID(),
            type = NotificationType.EMAIL,
            recipient = "test@example.com",
            templateId = null,
            subject = "Test Subject",
            body = "Test Body",
            data = emptyMap(),
            status = NotificationStatus.QUEUED,
            priority = Priority.NORMAL,
            scheduledAt = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            attempts = 0,
            lastAttemptAt = null,
            deliveredAt = null,
            errorMessage = null,
            provider = null,
            metadata = emptyMap()
        )

        coEvery { notificationRepository.create(any()) } returns notification
        coEvery { notificationRepository.createAuditLog(any()) } just Runs

        // When
        val response = notificationService.createNotification(request)

        // Then
        assertNotNull(response)
        assertEquals(NotificationStatus.QUEUED, response.status)
        coVerify(exactly = 1) { notificationRepository.create(any()) }
        coVerify(exactly = 1) { notificationRepository.createAuditLog(any()) }
    }

    @Test
    fun `processNotification should deliver notification successfully`() = runBlocking {
        // Given
        val notification = Notification(
            id = UUID.randomUUID(),
            type = NotificationType.EMAIL,
            recipient = "test@example.com",
            templateId = null,
            subject = "Test",
            body = "Test Body",
            data = emptyMap(),
            status = NotificationStatus.QUEUED,
            priority = Priority.NORMAL,
            scheduledAt = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            attempts = 0,
            lastAttemptAt = null,
            deliveredAt = null,
            errorMessage = null,
            provider = null,
            metadata = emptyMap()
        )

        coEvery { emailProvider.send(any()) } returns ProviderResult(success = true)
        coEvery { emailProvider.getProviderName() } returns "email-smtp"
        coEvery { notificationRepository.update(any()) } returns notification
        coEvery { notificationRepository.createAuditLog(any()) } just Runs

        // When
        notificationService.processNotification(notification)

        // Then
        coVerify(atLeast = 1) { emailProvider.send(any()) }
        coVerify(atLeast = 1) { notificationRepository.update(any()) }
    }

    @Test
    fun `getNotificationStatus should return status for existing notification`() = runBlocking {
        // Given
        val id = UUID.randomUUID()
        val notification = Notification(
            id = id,
            type = NotificationType.EMAIL,
            recipient = "test@example.com",
            templateId = null,
            subject = "Test",
            body = "Test Body",
            data = emptyMap(),
            status = NotificationStatus.DELIVERED,
            priority = Priority.NORMAL,
            scheduledAt = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            attempts = 1,
            lastAttemptAt = Instant.now(),
            deliveredAt = Instant.now(),
            errorMessage = null,
            provider = "email-smtp",
            metadata = emptyMap()
        )

        coEvery { notificationRepository.findById(id) } returns notification

        // When
        val status = notificationService.getNotificationStatus(id)

        // Then
        assertNotNull(status)
        assertEquals(NotificationStatus.DELIVERED, status.status)
        assertEquals(1, status.attempts)
    }
}
