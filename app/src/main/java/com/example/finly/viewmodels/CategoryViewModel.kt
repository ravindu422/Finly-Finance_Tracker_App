package com.example.finly.viewmodels

import android.app.Application
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import com.example.finly.R
import com.example.finly.models.Category
import com.example.finly.models.Transaction
import com.example.finly.models.TransactionType
import com.example.finly.repositories.TransactionRepository

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransactionRepository(application)

    fun getCategorySpending(transaction: List<Transaction>): Map<String, Double> {
        val transactions = repository.getTransactions()
        return  transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, transactions) ->
                transactions.sumOf { it.amount }
            }
    }

    fun getCategories(): List<Category> {
        return listOf(
            Category(name = "Food", icon = R.drawable.ic_food, color = Color.parseColor("#FF5722")),
            Category(name = "Transport", icon = R.drawable.ic_transport, color = Color.parseColor("#2196F3")),
            Category(name = "Bills", icon = R.drawable.ic_bills, color = Color.parseColor("#4CAF50")),
            Category(name = "Entertainment", icon = R.drawable.ic_entertainment, color = Color.parseColor("#9C27B0")),
            Category(name = "Shopping", icon = R.drawable.ic_shopping, color = Color.parseColor("#FF9800")),
            Category(name = "Health", icon = R.drawable.ic_health, color = Color.parseColor("#E91E63")),
            Category(name = "Other", icon = R.drawable.ic_other, color = Color.parseColor("#607D8B"))
        )
    }
}