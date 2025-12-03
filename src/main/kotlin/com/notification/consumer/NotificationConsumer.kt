package com.notification.consumer

import com.notification.config.KafkaConfig
import com.notification.repository.NotificationRepository
import com.notification.service.NotificationService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.*

private val logger = KotlinLogging.logger {}

class NotificationConsumer(
    private val kafkaConfig: KafkaConfig,
    private val notificationService: NotificationService,
    private val notificationRepository: NotificationRepository
) {
    private var job: Job? = null
    private lateinit var consumer: KafkaConsumer<String, String>

    fun start(scope: CoroutineScope) {
        val props = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConfig.groupId)
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaConfig.enableAutoCommit)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        }

        consumer = KafkaConsumer(props)
        consumer.subscribe(listOf(kafkaConfig.topic))

        job = scope.launch {
            logger.info { "Kafka consumer started for topic: ${kafkaConfig.topic}" }

            try {
                while (isActive) {
                    val records = consumer.poll(Duration.ofMillis(1000))

                    records.forEach { record ->
                        try {
                            logger.debug { "Received message: ${record.value()}" }
                            // In production, deserialize the JSON and process
                            // For now, we'll process pending notifications from DB
                            processQueuedNotifications()
                        } catch (e: Exception) {
                            logger.error(e) { "Error processing message: ${record.value()}" }
                        }
                    }

                    if (!kafkaConfig.enableAutoCommit) {
                        consumer.commitSync()
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Kafka consumer error" }
            }
        }
    }

    private suspend fun processQueuedNotifications() {
        val pendingNotifications = notificationRepository.findPendingNotifications(10)

        pendingNotifications.forEach { notification ->
            try {
                notificationService.processNotification(notification)
            } catch (e: Exception) {
                logger.error(e) { "Error processing notification ${notification.id}" }
            }
        }
    }

    fun stop() {
        job?.cancel()
        consumer.close()
        logger.info { "Kafka consumer stopped" }
    }
}
