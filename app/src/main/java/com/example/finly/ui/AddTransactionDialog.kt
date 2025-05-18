package com.example.finly.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.app.NotificationCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.finly.MainActivity
import com.example.finly.R
import com.example.finly.databinding.DialogAddTransactionBinding
import com.example.finly.models.AppNotification
import com.example.finly.models.NotificationType
import com.example.finly.models.Transaction
import com.example.finly.models.TransactionType
import com.example.finly.repositories.BudgetRepository
import com.example.finly.repositories.NotificationRepository
import com.example.finly.repositories.TransactionRepository
import com.example.finly.repositories.UserPreferencesRepository
import com.example.finly.viewmodels.CategoryViewModel
import com.example.finly.viewmodels.TransactionViewModel
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddTransactionDialog : DialogFragment() {
    private var _binding: DialogAddTransactionBinding? = null;
    private val binding get() = _binding!!

    private lateinit var transactionViewModel: TransactionViewModel
    private lateinit var categoryViewModel: CategoryViewModel
    private var selectedCategory: String = "Other"
    private var selectDate: Date = Date()
    private var transactionType: TransactionType = TransactionType.EXPENSE
    private var existingTransaction: Transaction? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            val params = attributes
            val margin = (16 * resources.displayMetrics.density).toInt()
            params.width = resources.displayMetrics.widthPixels - (2 * margin)
            attributes = params
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionViewModel = ViewModelProvider(requireActivity())[TransactionViewModel::class.java]
        categoryViewModel = ViewModelProvider(requireActivity())[CategoryViewModel::class.java]

        existingTransaction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_TRANSACTION, Transaction::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable<Transaction>(ARG_TRANSACTION)
        }

        updateDialogAppearance()
        setupCategoryDropdown()
        setupDatePicker()
        setupTransactionTypeTab()
        setupButtons()

        existingTransaction?.let { fillFormWithTransactionData(it) }
    }

    private fun updateDialogAppearance() {
        if (existingTransaction != null) {
            binding.tvDialogTitle.text = getString(R.string.update_transaction)
            binding.btnSave.text = getString(R.string.btn_update)
        } else {
            binding.tvDialogTitle.text = getString(R.string.add_transaction)
            binding.btnSave.text = getString(R.string.btn_save)
        }
    }

    private fun fillFormWithTransactionData(transaction: Transaction) {
        binding.etTitle.setText(transaction.title)
        binding.etAmount.setText(transaction.amount.toString())
        binding.actCategory.setText(transaction.category, false)
        binding.etDate.setText(SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(transaction.date))
        binding.etNote.setText(transaction.note)

        selectedCategory = transaction.category
        selectDate = transaction.date
        transactionType = transaction.type

        binding.tabTransactionType.getTabAt(
            if (transaction.type == TransactionType.EXPENSE) 0 else 1
        )?.select()
    }

    private fun setupCategoryDropdown() {
        val categories = categoryViewModel.getCategories()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories.map { it.name }
        )

        binding.actCategory.setAdapter(adapter)
        binding.actCategory.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            selectedCategory = categories[position].name
        }
    }

    private fun setupDatePicker() {
        binding.etDate.setText(
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectDate)
        )

        binding.etDate.setOnClickListener {
            val calendar = Calendar.getInstance().apply {
                time = selectDate
            }

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectDate = calendar.time
                    binding.etDate.setText(
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectDate)
                    )
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupTransactionTypeTab() {
        binding.tabTransactionType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                transactionType = if (tab.position == 0) {
                    TransactionType.EXPENSE
                } else {
                    TransactionType.INCOME
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveTransaction()
        }
    }

    private fun saveTransaction() {
        val title = binding.etTitle.text.toString().trim()
        val amountStr = binding.etAmount.text.toString().trim()
        val note = binding.etNote.text.toString().trim()

        if (title.isEmpty()) {
            binding.etTitle.error = "Title is required"
            return
        }

        if (amountStr.isEmpty()) {
            binding.etAmount.error = "Amount is required"
            return
        }

        val amount = amountStr.toDoubleOrNull() ?: 0.0
        if (amount <= 0) {
            binding.etAmount.error = "Amount must be greater than 0"
            return
        }

        val transaction = existingTransaction?.copy(
            title = title,
            amount = amount,
            category = selectedCategory,
            type = transactionType,
            date = selectDate,
            note = note
        ) ?: Transaction(
            title = title,
            amount = amount,
            category = selectedCategory,
            type = transactionType,
            date = selectDate,
            note = note
        )

        val notificationRepository = NotificationRepository(requireContext())
        if (existingTransaction != null) {
            transactionViewModel.updateTransaction(transaction)

            val notificationTitle = "Transaction Updated"
            val notificationMessage = "$title for $${amount.formatAmount()} has been updated"

            val notification = AppNotification(
                title = notificationTitle,
                message = notificationMessage,
                type = NotificationType.TRANSACTION
            )

            notificationRepository.addNotification(notification)
        } else {
            transactionViewModel.addTransaction(transaction)

            val notificationTitle = if (transactionType == TransactionType.INCOME) "Income Added" else "Expense Recorded"
            val notificationMessage = "$title for $${amount.formatAmount()} has been added"

            val notification = AppNotification(
                title = notificationTitle,
                message = notificationMessage,
                type = NotificationType.TRANSACTION
            )

            notificationRepository.addNotification(notification)
        }

        if (transactionType == TransactionType.EXPENSE) {
            checkBudgetStatus(requireContext())
        }

        dismiss()
    }

    private fun checkBudgetStatus(context: Context) {
        val userPrefs = UserPreferencesRepository(context)
        if (!userPrefs.getBudgetAlertsPreference()) {
            return
        }

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

    private fun createInAppNotification(title: String, message: String, type: NotificationType) {
        val notificationRepository = NotificationRepository(requireContext())

        val notification = AppNotification(
            title = title,
            message = message,
            type = type
        )

        notificationRepository.addNotification(notification)
    }

    companion object {
        private const val ARG_TRANSACTION = "transaction"

        fun newInstance(transaction: Transaction? = null): AddTransactionDialog {
            return if (transaction != null) {
                val args = Bundle().apply {
                    putParcelable(ARG_TRANSACTION, transaction)
                }
                AddTransactionDialog().apply {
                    arguments = args
                }
            } else {
                AddTransactionDialog()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}