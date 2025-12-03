package com.notification.provider

import com.notification.config.EmailProviderConfig
import com.notification.domain.Notification
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

private val logger = KotlinLogging.logger {}

class EmailProvider(private val config: EmailProviderConfig) : NotificationProvider {

    private val session: Session by lazy {
        val props = Properties().apply {
            put("mail.smtp.host", config.host)
            put("mail.smtp.port", config.port.toString())
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.ssl.protocols", "TLSv1.2")
        }

        Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(config.username, config.password)
            }
        })
    }

    override suspend fun send(notification: Notification): ProviderResult = withContext(Dispatchers.IO) {
        try {
            if (!config.enabled) {
                logger.warn { "Email provider is disabled" }
                return@withContext ProviderResult(
                    success = false,
                    message = "Email provider is disabled",
                    errorCode = "PROVIDER_DISABLED"
                )
            }

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(config.from))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(notification.recipient))
                subject = notification.subject ?: "Notification"
                setText(notification.body, "UTF-8", "html")
            }

            Transport.send(message)

            logger.info { "Email sent successfully to ${notification.recipient}" }
            ProviderResult(success = true, message = "Email sent successfully")
        } catch (e: MessagingException) {
            logger.error(e) { "Failed to send email to ${notification.recipient}" }
            ProviderResult(
                success = false,
                message = e.message ?: "Failed to send email",
                errorCode = "MESSAGING_ERROR"
            )
        } catch (e: Exception) {
            logger.error(e) { "Unexpected error sending email to ${notification.recipient}" }
            ProviderResult(
                success = false,
                message = e.message ?: "Unexpected error",
                errorCode = "UNKNOWN_ERROR"
            )
        }
    }

    override suspend fun healthCheck(): Boolean {
        return try {
            config.enabled && config.host.isNotEmpty()
        } catch (e: Exception) {
            logger.error(e) { "Email provider health check failed" }
            false
        }
    }

    override fun getProviderName(): String = "email-smtp"
}
