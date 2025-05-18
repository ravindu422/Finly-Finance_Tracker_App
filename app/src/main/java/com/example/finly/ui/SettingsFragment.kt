package com.example.finly.ui

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.DrawableCompat.applyTheme
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.finly.R
import com.example.finly.databinding.FragmentSettingsBinding
import com.example.finly.repositories.BudgetRepository
import com.example.finly.repositories.TransactionRepository
import com.example.finly.repositories.UserPreferencesRepository
import com.example.finly.services.BackupService
import com.example.finly.services.BudgetCheckReceiver
import com.example.finly.services.NotificationService
import com.example.finly.services.ReminderReceiver
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File

class SettingsFragment: Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var backupService: BackupService

    private val importFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val success = backupService.importTransactions(uri)
                    if (success) {
                        Snackbar.make(binding.root, "Data imported successfully", Snackbar.LENGTH_LONG).show()

                        findNavController().navigate(R.id.action_global_dashboardFragment)
                    } else {
                        Snackbar.make(binding.root, "Failed to import data", Snackbar.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Import error: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreferencesRepository = UserPreferencesRepository(requireContext())
        backupService = BackupService(requireContext())

        val serviceIntent = Intent(requireContext(), NotificationService::class.java)
        requireContext().startService(serviceIntent)

        setupToolbar()
        setupCurrencySelector()
        setupThemeToggle()
        setupNotificationToggle()
        setupDataManagement()
        setupBudgetAlertToggle()
    }

    private fun setupToolbar() {
        view?.findViewById<ImageButton>(R.id.btn_back)?.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupCurrencySelector() {
        binding.tvCurrency.text = userPreferencesRepository.getCurrency()

        binding.layoutCurrency.setOnClickListener {
            val currencies = arrayOf("USD", "EUR", "JPY", "CAD", "LKR")
            val currentCurrency = userPreferencesRepository.getCurrency()
            val currentSelection = currencies.indexOf(currentCurrency)

            // Check if we're in dark mode
            val isNightMode = (requireContext().resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            // Create the dialog
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Currency")
                .setSingleChoiceItems(currencies, currentSelection) { dlg, which ->
                    val selectedCurrency = currencies[which]
                    userPreferencesRepository.saveCurrency(selectedCurrency)
                    Toast.makeText(requireContext(), "Currency set to $selectedCurrency", Toast.LENGTH_SHORT).show()
                    binding.tvCurrency.text = selectedCurrency
                    dlg.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .create()

            // Show the dialog first
            dialog.show()

            // Then customize button colors after the dialog is shown
            if (isNightMode) {
                // In dark mode, use white for the cancel button
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.WHITE)
            } else {
                // In light mode, use your app's accent color for a branded look
                val accentColor = ContextCompat.getColor(requireContext(), R.color.accent)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(accentColor)
            }
        }
    }

    private fun setupThemeToggle() {
        val isDarkMode = userPreferencesRepository.getDarkModePreference()
        binding.switchDarkMode.isChecked = isDarkMode

        binding.tvTheme.text = if (isDarkMode) "Dark mode" else "Light mode"

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            userPreferencesRepository.savedDarkModePreference(isChecked)
            binding.tvTheme.text = if (isChecked) "Dark mode" else "Light mode"

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            val message = if (isChecked) "Dark mode enabled" else "Light mode disabled"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        binding.layoutTheme.setOnClickListener {
            binding.switchDarkMode.toggle()
        }
    }

    private fun applyTheme(isDarkMode: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun setupNotificationToggle() {
        val sharedPreferences = requireContext().getSharedPreferences(
            "notification_prefs", Context.MODE_PRIVATE
        )
        val notificationsEnabled = sharedPreferences.getBoolean("daily_reminder", true)

        binding.switchNotifications.isChecked = notificationsEnabled

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("daily_reminder", isChecked).apply()

            val serviceIntent = Intent(requireContext(), NotificationService::class.java)

            if (isChecked) {
                serviceIntent.action = "ACTION_SCHEDULE_REMINDER"
                requireContext().startService(serviceIntent)
                Toast.makeText(requireContext(), "Daily reminders enabled", Toast.LENGTH_SHORT).show()
            } else {
                val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(requireContext(), ReminderReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    requireContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
                Toast.makeText(requireContext(), "Daily reminders disabled", Toast.LENGTH_SHORT).show()
            }
        }

        binding.layoutNotification.setOnClickListener {
            binding.switchNotifications.toggle()
        }
    }

    private fun setupBudgetAlertToggle() {
        val budgetAlertsEnabled = userPreferencesRepository.getBudgetAlertsPreference()

        binding.switchBudgetAlerts.isChecked = budgetAlertsEnabled

        binding.switchBudgetAlerts.setOnCheckedChangeListener { _, isChecked ->
            userPreferencesRepository.saveBudgetAlertsPreference(isChecked)

            val serviceIntent = Intent(requireContext(), NotificationService::class.java)

            if (isChecked) {
                serviceIntent.action = "ACTION_CHECK_BUDGET"
                requireContext().startService(serviceIntent)

                scheduleBudgetChecks()

                Toast.makeText(requireContext(), "Budget alerts enabled", Toast.LENGTH_SHORT).show()
            } else {
                cancelBudgetChecks()

                Toast.makeText(requireContext(), "Budget alerts disabled", Toast.LENGTH_SHORT).show()
            }
        }

        binding.layoutBudgetAlerts.setOnClickListener {
            binding.switchBudgetAlerts.toggle()
        }
    }

    private fun scheduleBudgetChecks() {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(requireContext(), BudgetCheckReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(), 1, intent, PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis(),
            6 * 60 * 60 * 1000,
            pendingIntent
        )
    }

    private fun cancelBudgetChecks() {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), BudgetCheckReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(), 1, intent, PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun setupDataManagement() {
         binding.btnExport.setOnClickListener {
             try {
                 val file = backupService.exportTransactions()

                 val message = "Data exported successfully to:\n${file.absolutePath}"
                 Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()

                 shareBackupFile(file)
             } catch (e: Exception) {
                 Snackbar.make(binding.root, "Export failed: ${e.message}", Snackbar.LENGTH_LONG).show()
             }
         }

        binding.btnImport.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            importFileLauncher.launch(intent)
        }

        binding.layoutClearData.setOnClickListener {
            // Check if we're in dark mode
            val isNightMode = (requireContext().resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            // Create the dialog
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear All Data")
                .setMessage("Are you sure you want to delete all your financial data? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    clearAllData()
                    Snackbar.make(binding.root, "All data has been cleared", Snackbar.LENGTH_LONG).show()
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

    private fun shareBackupFile(file: File) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "application/json"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share backup file"))
    }

    private fun clearAllData() {
        val transactionRepository = TransactionRepository(requireContext())
        transactionRepository.saveTransactions(emptyList())

        val budgetRepository = BudgetRepository(requireContext())
        budgetRepository.saveMonthlyBudget(0.0)

        requireActivity().findNavController(R.id.nav_host_fragment)
            .navigate(R.id.action_global_dashboardFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_IMPORT_FILE = 123
    }

}