package com.example.finly.ui

import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finly.R
import com.example.finly.databinding.FragmentNotificationsBinding
import com.example.finly.repositories.NotificationRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class NotificationsFragment : Fragment() {
    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var notificationRepository: NotificationRepository
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notificationRepository = NotificationRepository(requireContext())

        setupToolbar()
        setupRecyclerView()
        loadNotifications()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnClearAll.setOnClickListener {
            showClearAllConfirmationDialog()
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            onMarkAsRead = { notification ->
                if (!notification.isRead) {
                    notificationRepository.markAsRead(notification.id)
                    loadNotifications()
                }
            },
            onDelete = { notification ->
                notificationRepository.deleteNotification(notification.id)
                loadNotifications()
            }
        )

        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationAdapter
            setHasFixedSize(true)
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val notification = notificationAdapter.currentList[position]
                if (!notification.isRead) {
                    notificationRepository.markAsRead(notification.id)
                }
                loadNotifications()
            }

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val position = viewHolder.adapterPosition
                val notification = notificationAdapter.currentList[position]
                return if (notification.isRead) {
                    0
                } else {
                    super.getSwipeDirs(recyclerView, viewHolder)
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView

                // Skip drawing if the swipe distance is zero
                if (dX == 0f) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    return
                }

                // Define colors - use modern green for "mark as read"
                val isNightMode = (requireContext().resources.configuration.uiMode and
                        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

                val readColor = if (isNightMode) {
                    Color.parseColor("#81C784") // Slightly darker green for dark mode
                } else {
                    Color.parseColor("#4CAF50") // Material Design green 500
                }

                // Set up the background with rounded corners on the right side
                val background = GradientDrawable()
                background.shape = GradientDrawable.RECTANGLE
                background.setColor(readColor)

                // Apply corner radius only to right side (if swiping left-to-right)
                if (dX > 0) {
                    background.cornerRadii = floatArrayOf(
                        24f, 24f,       // top-left
                        0f, 0f,         // top-right
                        0f, 0f,         // bottom-right
                        24f, 24f        // bottom-left
                    )
                }

                // Draw the background
                if (dX > 0) {
                    background.setBounds(
                        itemView.left,
                        itemView.top + 8,   // Add a small margin
                        itemView.left + dX.toInt(),
                        itemView.bottom - 8 // Add a small margin
                    )
                }

                // Draw the background first
                background.draw(c)

                // Draw the "Mark as Read" text
                if (dX > 0) { // Swiping to the right
                    // Inside the onChildDraw method, update the textPaint configuration:
                    val textPaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 30f  // Smaller text size
                        typeface = Typeface.create("poppins_medium", Typeface.NORMAL)
                        textAlign = Paint.Align.LEFT
                        isAntiAlias = true  // Enable anti-aliasing for smooth text
                        isDither = true     // Enable dithering for better color rendering
                        isFilterBitmap = true // Apply filtering for smoother text
                        isSubpixelText = true // Enable subpixel text rendering for sharper text
                    }

                    // Calculate text position
                    val textY = (itemView.top + itemView.height / 2 + (textPaint.textSize / 3)).toInt()
                    val textX = itemView.left + 40 // Margin from the left edge

                    c.drawText("Mark as Read", textX.toFloat(), textY.toFloat(), textPaint)
                }

                // Add a subtle fade effect based on swipe distance
                val alpha = 1.0f - Math.abs(dX) / itemView.width.toFloat()
                itemView.alpha = alpha
                itemView.translationX = dX

                // No need to call super as we're handling translation manually
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.rvNotifications)
    }

    private fun loadNotifications() {
        val notifications = notificationRepository.getNotifications()
        notificationAdapter.submitList(notifications)

        if (notifications.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvNotifications.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvNotifications.visibility = View.VISIBLE
        }
    }

    private fun showClearAllConfirmationDialog() {
        // Check if we're in dark mode
        val isNightMode = (requireContext().resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // Create the dialog
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear All Notifications")
            .setMessage("Are you sure you want to delete all notifications?")
            .setPositiveButton("Clear All") { _, _ ->
                notificationRepository.clearAllNotifications()
                loadNotifications()
            }
            .setNegativeButton("Cancel", null)
            .create()

        // Show the dialog first
        dialog.show()

        // Then customize button colors after the dialog is shown
        if (isNightMode) {
            // In dark mode, set text color to white for better visibility
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.WHITE)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.WHITE)
        } else {
            // In light mode, use your app's accent color
            val accentColor = ContextCompat.getColor(requireContext(), R.color.accent)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(accentColor)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(accentColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}