package com.example.finly.services

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.finly.MainActivity
import com.example.finly.R
import com.example.finly.models.AppNotification
import com.example.finly.models.NotificationType
import com.example.finly.models.TransactionType
import com.example.finly.repositories.BudgetRepository
import com.example.finly.repositories.NotificationRepository
import com.example.finly.repositories.TransactionRepository
import com.example.finly.repositories.UserPreferencesRepository
import java.util.Calendar

class NotificationService : Service() {
    private lateinit var notificationManager: NotificationManager
    private val channelId = "budget_alerts"
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var transactionRepository: TransactionRepository

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        budgetRepository = BudgetRepository(applicationContext)
        transactionRepository = TransactionRepository(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("NotificationService", "Service started with action: ${intent?.action}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            intent?.action == "ACTION_CHECK_BUDGET") {
            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Finly")
                .setContentText("Checking budget status")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            startForeground(999, notification)

            Handler(Looper.getMainLooper()).postDelayed({
                checkAndNotifyBudgetStatus()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    stopForeground(true)
                }
                stopSelf(startId)
            }, 200)

            return START_NOT_STICKY
        }

        when (intent?.action) {
            "ACTION_SCHEDULE_REMINDER" -> scheduleDailyReminder()
            "ACTION_CHECK_BUDGET" -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    checkAndNotifyBudgetStatus()
                    stopSelf(startId)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts about budget limits"
                enableLights(true)
                lightColor = Color.RED
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getCurrentMonthExpenses(): Double {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return transactionRepository.getTransactions()
            .filter { transaction ->
                val transactionCalendar = Calendar.getInstance().apply {
                    time = transaction.date
                }
                transaction.type == TransactionType.EXPENSE &&
                        transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                        transactionCalendar.get(Calendar.YEAR) == currentYear
            }
            .sumOf { it.amount }
    }

    private fun getBudgetUsagePercentage(): Double {
        val budget = budgetRepository.getMonthlyBudget()
        if (budget == 0.0) return 0.0

        val expenses = getCurrentMonthExpenses()
        return (expenses / budget) * 100
    }

    private fun checkAndNotifyBudgetStatus() {
        Log.d("NotificationService", "Checking budget status")
        val preferences = UserPreferencesRepository(applicationContext)

        if (!preferences.getBudgetAlertsPreference()) {
            Log.d("NotificationService", "Budget alerts disabled")
            return
        }

        val budget = budgetRepository.getMonthlyBudget()
        Log.d("NotificationService", "Monthly budget: $budget")

        val expenses = getCurrentMonthExpenses()
        Log.d("NotificationService", "Current month expenses: $expenses")

        if (budget == 0.0) {
            Log.d("NotificationService", "No budget set, skipping check")
            return
        }

        val usagePercentage = (expenses / budget) * 100
        Log.d("NotificationService", "Budget usage: $usagePercentage%")

        val currency = preferences.getCurrency()
        val remaining = budget - expenses

        when {
            usagePercentage >= 100 -> {
                Log.d("NotificationService", "Budget exceeded, sending notification")
                val title = "Budget Exceeded!"
                val content = "You've exceeded your monthly budget of $currency$budget by $currency${expenses - budget}"
                sendBudgetNotification(title, content)
                createInAppNotification(title, content, NotificationType.BUDGET_ALERT)
            }
            usagePercentage >= 75 -> {
                Log.d("NotificationService", "75% budget threshold reached, sending notification")
                val title = "Budget Alert!"
                val content = "You've used ${usagePercentage.toInt()}% of your monthly budget. $currency$remaining remaining."
                sendBudgetNotification(title, content)
                createInAppNotification(title, content, NotificationType.BUDGET_ALERT)
            }
            else -> {
                Log.d("NotificationService", "No budget threshold reached, no notification needed")
            }
        }
    }

    private fun createInAppNotification(title: String, message: String, type: NotificationType) {
        val notificationRepository = NotificationRepository(this)

        val notification = AppNotification(
            title = title,
            message = message,
            type = type
        )

        notificationRepository.addNotification(notification)
    }

    private fun sendBudgetNotification(title: String, content:String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    fun scheduleDailyReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(applicationContext, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
