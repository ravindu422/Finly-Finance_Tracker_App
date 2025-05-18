package com.example.finly.models

import java.util.UUID

data class AppNotification(
    val id:String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
