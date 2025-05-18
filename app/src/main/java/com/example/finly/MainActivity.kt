package com.example.finly

import android.os.Build
import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager

import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.finly.databinding.ActivityMainBinding
import com.example.finly.services.NotificationService
import com.example.finly.ui.AddTransactionDialog
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.finly.repositories.UserPreferencesRepository

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val userPrefs = UserPreferencesRepository(this)
        val isDarkMode = userPrefs.getDarkModePreference()
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        binding.fabAddTransaction.setOnClickListener {
            showAddTransactionDialog()
        }

        // Request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            } else {
                // Permission granted, schedule reminder
                scheduleReminderFromActivity()
            }
        } else {
            // On older Android versions
            scheduleReminderFromActivity()
        }
    }

    private fun scheduleReminderFromActivity() {
        // Start service to schedule reminder
        val serviceIntent = Intent(this, NotificationService::class.java)
        serviceIntent.action = "ACTION_SCHEDULE_REMINDER"
        startService(serviceIntent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, schedule reminder
            scheduleReminderFromActivity()
        }
    }

    private fun showAddTransactionDialog() {
        val dialog = AddTransactionDialog()
        dialog.show(supportFragmentManager, "AddTransactionDialog")
    }
}