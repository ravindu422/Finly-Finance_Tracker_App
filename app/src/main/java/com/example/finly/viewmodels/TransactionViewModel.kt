package com.example.finly.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.finly.models.AppNotification
import com.example.finly.models.NotificationType
import com.example.finly.models.Transaction
import com.example.finly.models.TransactionType
import com.example.finly.repositories.BudgetRepository
import com.example.finly.repositories.NotificationRepository
import com.example.finly.repositories.TransactionRepository
import com.example.finly.utils.formatAmount
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionRepository = TransactionRepository(application)
    private val _transactions = MutableLiveData<List<Transaction>>()

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        _transactions.value = transactionRepository.getTransactions()
    }

    fun getAllTransactions(): LiveData<List<Transaction>> {
        return _transactions
    }

    fun getRecentTransactions(limit: Int = 5): LiveData<List<Transaction>> {
        val result = MediatorLiveData<List<Transaction>>()

        result.addSource(_transactions) { transactions ->
            result.value = transactions
                .sortedByDescending { it.date}
                .take(limit)
        }
        return result
    }

    fun addTransaction(transaction: Transaction) {
        transactionRepository.addTransaction(transaction)
        loadTransactions()

        val notificationRepository = NotificationRepository(getApplication())

        val isIncome = transaction.type == TransactionType.INCOME
        val title = if (isIncome) "Income Added" else "Expense Recorded"
        val message = "$${transaction.amount.formatAmount()} for ${transaction.category}"

        val notification = AppNotification(
            title = title,
            message = message,
            type = NotificationType.TRANSACTION
        )

        notificationRepository.addNotification(notification)
    }

    fun updateTransaction(transaction: Transaction) {
        transactionRepository.updateTransaction(transaction)
        loadTransactions()
    }

    fun deleteTransaction(transaction: Transaction) {
        transactionRepository.deleteTransaction(transaction.id)
        loadTransactions()
    }

    fun getBudgetLiveData(): LiveData<Double> {
        val budgetRepository = BudgetRepository(getApplication())
        val result = MutableLiveData<Double>()
        result.value = budgetRepository.getMonthlyBudget()
        return result
    }

    fun getTransactionsByType(type: TransactionType): LiveData<List<Transaction>> {
        return transactionRepository.getTransactionsByType(type)
    }
}