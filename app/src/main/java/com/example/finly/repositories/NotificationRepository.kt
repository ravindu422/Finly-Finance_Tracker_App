package com.example.finly.repositories

import android.content.Context
import com.example.finly.models.AppNotification
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class NotificationRepository(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(
        "notification_data_prefs", Context.MODE_PRIVATE
    )

    private val gson = Gson()

    private val listeners = mutableSetOf<() -> Unit>()

    fun addNotificationsChangeListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeNotificationsChangeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyNotificationsChanged() {
        listeners.forEach { it.invoke() }
    }

    fun getNotifications(): List<AppNotification> {
        val json = sharedPreferences.getString("app_notifications", null) ?: return emptyList()
        val type = object : TypeToken<List<AppNotification>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveNotifications(notifications: List<AppNotification>) {
        val json = gson.toJson(notifications)
        sharedPreferences.edit().putString("app_notifications", json).apply()
        notifyNotificationsChanged()
    }

    fun addNotification(notification: AppNotification) {
        val notifications = getNotifications().toMutableList()
        notifications.add(0, notification)
        saveNotifications(notifications)
    }

    fun markAsRead(notificationId: String) {
        val notifications = getNotifications().toMutableList()
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            notifications[index] = notifications[index].copy(isRead = true)
            saveNotifications(notifications)
        }
    }

    fun markAllAsRead() {
        val notifications = getNotifications().map {it.copy(isRead = true)}
        saveNotifications(notifications)
    }

    fun deleteNotification(notificationId: String) {
        val notifications = getNotifications().toMutableList()
        notifications.removeAll{ it.id == notificationId }
        saveNotifications(notifications)
    }

    fun clearAllNotifications() {
        saveNotifications(emptyList())
    }

    fun getUnreadCount(): Int {
        return getNotifications().count { !it.isRead }
    }
}