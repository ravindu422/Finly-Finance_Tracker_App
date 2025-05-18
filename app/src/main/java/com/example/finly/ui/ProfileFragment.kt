package com.example.finly.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Patterns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.finly.AuthActivity
import com.example.finly.R
import com.example.finly.databinding.FragmentProfileBinding
import com.example.finly.models.TransactionType
import com.example.finly.repositories.TransactionRepository
import com.example.finly.repositories.UserPreferencesRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar
import kotlin.math.abs

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var transactionRepository: TransactionRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionRepository = TransactionRepository(requireContext())

        loadUserProfile()
        setupProfileSummary()
        setupButtons()
    }

    private fun loadUserProfile() {
        val sharedPrefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userName = sharedPrefs.getString("user_name", "User") ?: "User"
        val userEmail = sharedPrefs.getString("user_email", "user@example.com") ?: "user@example.com"

        val memberSince = sharedPrefs.getString("member_since", "April 2025") ?: "April 2025"

        binding.tvUserName.text = userName
        binding.tvUserEmail.text = userEmail
        binding.tvMemberSince.text = memberSince
    }

    private fun setupProfileSummary() {
        val transactions = transactionRepository.getTransactions()

        binding.tvTransactionsCount.text = transactions.size.toString()

        if (binding.tvTotalBalance != null) {
            val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            val balance = totalIncome - totalExpense

            val userPreferencesRepository = try {
                UserPreferencesRepository(requireContext())
            } catch (e: Exception) {
                null
            }

            val currency = userPreferencesRepository?.getCurrency() ?: "USD"

            binding.tvTotalBalance.text = formatCurrency(abs(balance), currency)

            if (balance < 0) {
                binding.tvTotalBalance.setTextColor(Color.parseColor("#FF5A5F"))
                binding.ivBalanceWarning.visibility = View.VISIBLE
            } else {
                binding.tvTotalBalance.setTextColor(Color.parseColor("#0EA5E9"))
                binding.ivBalanceWarning.visibility = View.GONE
            }

            if (binding.tvMonthlySpending != null) {
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)

                val monthlyExpense = transactions
                    .filter { transaction ->
                        val transactionCalendar = Calendar.getInstance().apply {
                            time = transaction.date
                        }
                        transaction.type == TransactionType.EXPENSE &&
                                transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                                transactionCalendar.get(Calendar.YEAR) == currentYear
                    }
                    .sumOf { it.amount }

                binding.tvMonthlySpending.text = formatCurrency(monthlyExpense, currency)
            }
        }

        binding.tvLastLogin.text = "Today"
    }

    private fun formatCurrency(amount: Double, currency: String): String {
        return when (currency) {
            "USD" -> "$${String.format("%.2f", amount)}"
            "EUR" -> "€${String.format("%.2f", amount)}"
            "JPY" -> "¥${String.format("%.0f", amount)}"
            "LKR" -> "Rs.${String.format("%.2f", amount)}"
            else -> "$${String.format("%.2f", amount)}"
        }
    }

    private fun setupButtons() {
        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }
        binding.btnHelp.setOnClickListener {
            Toast.makeText(requireContext(), "Help & Support coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.btnPrivacy.setOnClickListener {
            Toast.makeText(requireContext(), "Privacy Policy coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.btnLogout.setOnClickListener {
            // Check if we're in dark mode
            val isNightMode = (requireContext().resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            // Create the dialog using MaterialAlertDialogBuilder for better material design styling
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    val sharedPrefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    sharedPrefs.edit().putBoolean("is_logged_in", false).apply()

                    startActivity(Intent(requireContext(), AuthActivity::class.java))
                    requireActivity().finish()
                }
                .setNegativeButton("Cancel", null)
                .create()

            // Show the dialog first
            dialog.show()

            // Then customize button colors after the dialog is shown
            if (isNightMode) {
                // In dark mode, use white for both buttons
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.WHITE)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.WHITE)
            } else {
                // In light mode, use your app's accent color for a branded look
                val accentColor = ContextCompat.getColor(requireContext(), R.color.accent)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(accentColor)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(accentColor)
            }
        }
    }

    private fun showEditProfileDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)
        dialog.setContentView(dialogView)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog.window?.attributes?.apply {
            val margin = (24 * resources.displayMetrics.density).toInt()
            width = resources.displayMetrics.widthPixels - (2 * margin)
        }

        val userPrefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val currentName = userPrefs.getString("user_name", "")
        val currentEmail = userPrefs.getString("user_email", "")

        val nameEditText = dialogView.findViewById<TextInputEditText>(R.id.et_name)
        val emailEditText = dialogView.findViewById<TextInputEditText>(R.id.et_email)

        nameEditText.setText(currentName)
        emailEditText.setText(currentEmail)

        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val saveButton = dialogView.findViewById<MaterialButton>(R.id.btn_save)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        saveButton.setOnClickListener {
            val newName = nameEditText.text.toString().trim()
            val newEmail = emailEditText.text.toString().trim()

            if (newName.isEmpty()) {
                nameEditText.error = "Name is required"
                return@setOnClickListener
            }

            if (newEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                emailEditText.error = "Valid email is required"
                return@setOnClickListener
            }

            userPrefs.edit().apply {
                putString("user_name", newName)
                putString("user_email", newEmail)
                apply()
            }

            binding.tvUserName.text = newName
            binding.tvUserEmail.text = newEmail

            dialog.dismiss()
            Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}