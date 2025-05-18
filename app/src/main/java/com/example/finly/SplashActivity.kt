package com.example.finly

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                checkUserStatus()
            } catch (e: Exception) {
                Log.e("SplashActivity", "Navigation error: ${e.message}", e)

                Toast.makeText(
                    this,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, 3000)
    }

    private fun checkUserStatus() {
        try {
            val appPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val userPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val onboardingCompleted = appPrefs.getBoolean("onboarding_completed", false)
            val isLoggedIn = userPrefs.getBoolean("is_logged_in", false)
            val intent = when {
                !onboardingCompleted -> Intent(this, OnboardingActivity::class.java)
                !isLoggedIn -> Intent(this, AuthActivity::class.java)
                else -> Intent(this, MainActivity::class.java)
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("SplashActivity", "Error in checkUserStatus: ${e.message}", e)
            throw e
        }
    }
}