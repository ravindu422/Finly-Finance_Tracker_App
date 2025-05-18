package com.example.finly.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
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

class BudgetCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val userPrefs = UserPreferencesRepository(context)
        if (!userPrefs.getBudgetAlertsPreference()) return

        val budgetRepository = BudgetRepository(context)
        val transactionRepository = TransactionRepository(context)

        val budget = budgetRepository.getMonthlyBudget()
        if (budget <= 0.0) return

        val expenses = getCurrentMonthExpenses(transactionRepository)
        val usagePercentage = (expenses / budget) * 100

        val currency = userPrefs.getCurrency()
        val remaining = budget - expenses

        when {
            usagePercentage >= 100 -> {
                val title = "Budget Exceeded!"
                val content = "You've exceeded your monthly budget of $currency$budget by $currency${expenses - budget}"

                sendBudgetNotification(context, title, content)

                val notification = AppNotification(
                    title = title,
                    message = content,
                    type = NotificationType.BUDGET_ALERT
                )
                NotificationRepository(context).addNotification(notification)
            }
            usagePercentage >= 75 -> {
                val title = "Budget Alert!"
                val content = "You've used ${usagePercentage.toInt()}% of your monthly budget. $currency$remaining remaining."

                sendBudgetNotification(context, title, content)

                val notification = AppNotification(
                    title = title,
                    message = content,
                    type = NotificationType.BUDGET_ALERT
                )
                NotificationRepository(context).addNotification(notification)
            }
        }
    }

    private fun getCurrentMonthExpenses(transactionRepository: TransactionRepository): Double {
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

    private fun sendBudgetNotification(context: Context, title: String, content: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "budget_alerts"
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

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "budget_alerts")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}