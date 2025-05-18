package com.example.finly.repositories

import android.content.Context

class BudgetRepository(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(
        "budget_prefs", Context.MODE_PRIVATE
    )

    fun saveMonthlyBudget(amount: Double) {
        sharedPreferences.edit().putFloat("monthly_budget", amount.toFloat()).apply()
    }

    fun getMonthlyBudget(): Double {
        return sharedPreferences.getFloat("monthly_budget", 0f).toDouble()
    }

    fun saveCategoryBudget(category: String, amount: Double) {
        sharedPreferences.edit().putFloat("budget_$category", amount.toFloat()).apply()
    }

    fun getCategoryBudget(category: String): Double {
        return sharedPreferences.getFloat("budget_$category", 0f).toDouble()
    }
}