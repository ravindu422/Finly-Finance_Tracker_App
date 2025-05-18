package com.example.finly.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.finly.MainActivity
import com.example.finly.R
import com.example.finly.models.AppNotification
import com.example.finly.models.NotificationType
import com.example.finly.repositories.NotificationRepository

class ReminderReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {

        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, NotificationService::class.java)
            serviceIntent.action = "ACTION_SCHEDULE_REMINDER"
            context.startService(serviceIntent)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "budget_alerts")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Daily Reminder")
            .setContentText("Don't Forget to record today's expenses!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, notification)

        val notificationRepository = NotificationRepository(context)

        val title = "Don't forget!"
        val message = "Remember to record today's transactions"

        val notifications = AppNotification(
            title = title,
            message = message,
            type = NotificationType.REMINDER
        )

        notificationRepository.addNotification(notifications)
    }
}