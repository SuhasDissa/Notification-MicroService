package com.notification.util

import com.notification.config.CircuitBreakerConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

private val logger = KotlinLogging.logger {}

enum class CircuitBreakerState {
    CLOSED, OPEN, HALF_OPEN
}

class CircuitBreaker(
    private val config: CircuitBreakerConfig,
    private val name: String = "default"
) {
    private var state: CircuitBreakerState = CircuitBreakerState.CLOSED
    private var failureCount = 0
    private var successCount = 0
    private var lastFailureTime: Instant? = null
    private val mutex = Mutex()

    suspend fun <T> execute(block: suspend () -> T): T {
        mutex.withLock {
            when (state) {
                CircuitBreakerState.OPEN -> {
                    val lastFailure = lastFailureTime
                    if (lastFailure != null &&
                        Instant.now().toEpochMilli() - lastFailure.toEpochMilli() > config.timeout
                    ) {
                        logger.info { "Circuit breaker $name transitioning to HALF_OPEN" }
                        state = CircuitBreakerState.HALF_OPEN
                        successCount = 0
                    } else {
                        throw CircuitBreakerOpenException("Circuit breaker $name is OPEN")
                    }
                }
                else -> {}
            }
        }

        return try {
            val result = block()
            onSuccess()
            result
        } catch (e: Exception) {
            onFailure()
            throw e
        }
    }

    private suspend fun onSuccess() = mutex.withLock {
        when (state) {
            CircuitBreakerState.HALF_OPEN -> {
                successCount++
                if (successCount >= config.successThreshold) {
                    logger.info { "Circuit breaker $name transitioning to CLOSED" }
                    state = CircuitBreakerState.CLOSED
                    failureCount = 0
                    successCount = 0
                }
            }
            CircuitBreakerState.CLOSED -> {
                failureCount = 0
            }
            else -> {}
        }
    }

    private suspend fun onFailure() = mutex.withLock {
        failureCount++
        lastFailureTime = Instant.now()

        when (state) {
            CircuitBreakerState.HALF_OPEN -> {
                logger.warn { "Circuit breaker $name transitioning to OPEN from HALF_OPEN" }
                state = CircuitBreakerState.OPEN
                successCount = 0
            }
            CircuitBreakerState.CLOSED -> {
                if (failureCount >= config.failureThreshold) {
                    logger.warn { "Circuit breaker $name transitioning to OPEN" }
                    state = CircuitBreakerState.OPEN
                }
            }
            else -> {}
        }
    }

    fun getState(): CircuitBreakerState = state
}

class CircuitBreakerOpenException(message: String) : Exception(message)
