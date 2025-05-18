package com.example.finly.ui

import android.app.Application
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.finly.R
import com.example.finly.databinding.ItemNotificationBinding
import com.example.finly.models.AppNotification
import com.example.finly.models.NotificationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationAdapter(
    private val onMarkAsRead: (AppNotification) -> Unit,
    private val onDelete: (AppNotification) -> Unit,
) : ListAdapter<AppNotification, NotificationAdapter.ViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    inner class ViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: AppNotification) {
            binding.tvNotificationTitle.text = notification.title
            binding.tvNotificationMessage.text = notification.message
            binding.tvNotificationTime.text = formatTimeAgo(notification.timestamp)

            itemView.isActivated = !notification.isRead
        }
    }

    private fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${diff / TimeUnit.MINUTES.toMillis(1)} mins ago"
            diff < TimeUnit.HOURS.toMillis(2) -> "1 hour ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${diff / TimeUnit.HOURS.toMillis(1)} hours ago"
            diff < TimeUnit.DAYS.toMillis(2) -> "1 day ago"
            diff < TimeUnit.DAYS.toMillis(7) -> "${diff / TimeUnit.DAYS.toMillis(1)} days ago"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AppNotification>() {
            override fun areItemsTheSame(oldItem: AppNotification, newItem: AppNotification): Boolean {
                return oldItem.id == newItem.id
            }
            override fun areContentsTheSame(oldItem: AppNotification, newItem: AppNotification): Boolean {
                return oldItem == newItem
            }
        }
    }
}