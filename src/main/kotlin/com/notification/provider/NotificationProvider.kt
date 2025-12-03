package com.notification.provider

import com.notification.domain.Notification

interface NotificationProvider {
    suspend fun send(notification: Notification): ProviderResult
    suspend fun healthCheck(): Boolean
    fun getProviderName(): String
}

data class ProviderResult(
    val success: Boolean,
    val message: String? = null,
    val errorCode: String? = null
)
