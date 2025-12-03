package com.notification.util

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay

private val logger = KotlinLogging.logger {}

class RetryHandler(
    private val maxRetries: Int,
    private val retryDelays: List<Long>
) {
    suspend fun <T> executeWithRetry(
        operation: String,
        block: suspend (attempt: Int) -> T
    ): T {
        var lastException: Exception? = null

        for (attempt in 0 until maxRetries) {
            try {
                logger.debug { "Executing $operation (attempt ${attempt + 1}/$maxRetries)" }
                return block(attempt)
            } catch (e: Exception) {
                lastException = e
                logger.warn(e) { "Failed $operation attempt ${attempt + 1}/$maxRetries" }

                if (attempt < maxRetries - 1) {
                    val delayMs = retryDelays.getOrElse(attempt) { retryDelays.last() }
                    logger.info { "Retrying $operation after ${delayMs}ms..." }
                    delay(delayMs)
                }
            }
        }

        logger.error { "All retry attempts exhausted for $operation" }
        throw lastException ?: Exception("Retry failed without exception")
    }
}
