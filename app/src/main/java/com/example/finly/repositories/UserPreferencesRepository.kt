package com.example.finly.repositories

import android.content.Context

class UserPreferencesRepository(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(
        "user_prefs", Context.MODE_PRIVATE
    )

    private val CURRENCY_KEY = "currency"

    fun saveCurrency(currency: String) {
        sharedPreferences.edit().putString(CURRENCY_KEY, currency).apply()
    }

    fun getBudgetAlertsPreference(): Boolean {
        return sharedPreferences.getBoolean("budget_alerts_enabled", false )
    }

    fun saveBudgetAlertsPreference(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("budget_alerts_enabled", enabled).apply()
    }

    fun getCurrency(): String {
        return sharedPreferences.getString("currency", "USD") ?: "USD"
    }

    fun savedDarkModePreference(isDarkMode: Boolean) {
        sharedPreferences.edit().putBoolean("dark_mode", isDarkMode).apply()
    }

    fun getDarkModePreference(): Boolean {
        return sharedPreferences.getBoolean("dark_mode", false)
    }
}