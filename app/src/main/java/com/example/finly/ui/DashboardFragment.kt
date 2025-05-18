package com.example.finly.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finly.R
import com.example.finly.databinding.FragmentDashboardBinding
import com.example.finly.models.Transaction
import com.example.finly.models.TransactionType
import com.example.finly.repositories.NotificationRepository
import com.example.finly.repositories.UserPreferencesRepository
import com.example.finly.services.BudgetCheckReceiver
import com.example.finly.utils.formatAmount
import com.example.finly.viewmodels.BudgetViewModel
import com.example.finly.viewmodels.TransactionViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.lang.Math.abs
import java.util.Calendar

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var transactionViewModel: TransactionViewModel
    private lateinit var budgetViewModel: BudgetViewModel
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionViewModel = ViewModelProvider(requireActivity())[TransactionViewModel::class.java]
        budgetViewModel = ViewModelProvider(requireActivity())[BudgetViewModel::class.java]
        budgetViewModel.loadBudget()

        setupGreeting()
        setupNotificationIcon()

        transactionAdapter = TransactionAdapter {

        }

        binding.tvViewAll.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_allTransactionsFragment)
        }

        binding.rvRecentTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }

        transactionViewModel.getRecentTransactions().observe(viewLifecycleOwner) { recentTransactions ->
            transactionAdapter.submitList(recentTransactions)
            updateSummary(recentTransactions)
        }

        budgetViewModel.budgetLiveData.observe(viewLifecycleOwner) { budget ->
            updateBudgetUI(budget)
        }

        budgetViewModel.expensesLiveData.observe(viewLifecycleOwner) { _ ->
            updateBudgetUI(budgetViewModel.getMonthlyBudget())
        }

        binding.cardBudget.setOnClickListener {
            showSetBudgetDialog()
        }

        binding.cardBudget.setOnLongClickListener {
            // Test budget check receiver
            val intent = Intent(requireContext(), BudgetCheckReceiver::class.java)
            requireContext().sendBroadcast(intent)

            Toast.makeText(requireContext(), "Testing budget notifications", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun setupGreeting() {
        binding.tvGreeting.text = getTimeBasedGreeting()
        val sharedPrefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userName = sharedPrefs.getString("user_name", "User") ?: "User"
        binding.tvUserName.text = userName
    }

    private fun getTimeBasedGreeting(): String {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        return when (hourOfDay) {
            in 5..11 -> "Good Morning,"
            in 12..16 -> "Good Afternoon,"
            in 17..20 -> "Good Evening,"
            else -> "Good Night,"
        }
    }

    private fun setupNotificationIcon() {
        val notificationIcon = binding.ivNotification
        val notificationBadge = view?.findViewById<View>(R.id.notification_badge)

        val notificationRepository = NotificationRepository(requireContext())
        val unreadCount = notificationRepository.getUnreadCount()
        notificationBadge?.visibility = if (unreadCount > 0) View.VISIBLE else View.GONE

        notificationIcon.setOnClickListener {
            try {
                Log.d("Navigation", "Attempting to navigate to notifications")
                findNavController().navigate(R.id.notificationsFragment)
            } catch (e: Exception) {
                Log.e("Navigation", "Error navigating: ${e.message}")
                Toast.makeText(requireContext(), "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        val notificationListener = {
            val newUnreadCount = notificationRepository.getUnreadCount()
            notificationBadge?.visibility = if (newUnreadCount > 0) View.VISIBLE else View.GONE
        }

        notificationRepository.addNotificationsChangeListener(notificationListener)

        viewLifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    notificationRepository.removeNotificationsChangeListener(notificationListener)
                }
            }
        })
    }

    private fun updateSummary(transactions: List<Transaction>) {
        val currentMonthTransactions = transactions.filter {
            val calendar = Calendar.getInstance().apply { time = it.date }
            val currentCalendar = Calendar.getInstance()

            calendar.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH) &&
                    calendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)
        }

        val income = currentMonthTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val expenses = currentMonthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val balance = income - expenses
        val currency = getCurrencySymbol()

        binding.tvIncome.text = "$currency${income.formatAmount()}"
        binding.tvExpenses.text = "$currency${expenses.formatAmount()}"
    }

    private fun updateBudgetUI(budget: Double) {
        val expenses = budgetViewModel.getCurrentMonthExpenses()
        val percentage = budgetViewModel.getBudgetUsagePercentage().toInt()
        val remaining = budgetViewModel.getBudgetRemaining()

        binding.tvBudgetAmount.text = "${getCurrencySymbol()}${budget.formatAmount()}"
        binding.progressBudget.progress = percentage
        binding.tvBudgetStatus.text = "$percentage% of budget used"
        binding.tvBudgetRemaining.text = "${getCurrencySymbol()}${remaining.formatAmount()}"

        val statusColor = when {
            percentage >= 100 -> Color.RED
            percentage >= 85 -> Color.parseColor("#FF6E00")
            percentage >= 65 -> Color.parseColor("#FFD801")
            else -> Color.parseColor("#3B82F6")
        }

        binding.tvBudgetRemaining.setTextColor(statusColor)
        binding.progressBudget.setIndicatorColor(statusColor)
    }

    private fun showSetBudgetDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_set_budget)

        dialog.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val editBudget = dialog.findViewById<TextInputEditText>(R.id.et_budget)
        val btnSaveBudget = dialog.findViewById<MaterialButton>(R.id.btn_save_budget)

        editBudget.setText(budgetViewModel.getMonthlyBudget().toString())

        btnSaveBudget.setOnClickListener {
            val budgetAmount = editBudget.text.toString().toDoubleOrNull() ?: 0.0
            budgetViewModel.setMonthlyBudget(budgetAmount)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getCurrencySymbol(): String {
        val preferencesRepository = UserPreferencesRepository(requireContext())
        return when (preferencesRepository.getCurrency()) {
            "USD" -> "$"
            "EUR" -> "€"
            "JPY" -> "¥"
            "CAD" -> "$"
            "LKR" -> "Rs."
            else -> "$"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}