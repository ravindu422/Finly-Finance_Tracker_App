package com.example.finly.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.finly.models.TransactionType
import com.example.finly.repositories.BudgetRepository
import com.example.finly.repositories.TransactionRepository
import java.util.Calendar

class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    private val budgetRepository = BudgetRepository(application)
    private val transactionRepository = TransactionRepository(application)

    private val _budgetLiveData = MutableLiveData<Double>()
    val budgetLiveData: LiveData<Double> get() = _budgetLiveData

    private val _expensesLiveData = MutableLiveData<Double>()
    val expensesLiveData: LiveData<Double> get() = _expensesLiveData

    private val transactionsChangeListener: () -> Unit = {
        _expensesLiveData.value = getCurrentMonthExpenses()
    }

    init {
        transactionRepository.addTransactionsChangeListener(transactionsChangeListener)

        loadBudget()
        _expensesLiveData.value = getCurrentMonthExpenses()
    }

    override fun onCleared() {
        super.onCleared()
        transactionRepository.removeTransactionsChangeListener(transactionsChangeListener)
    }

    fun loadBudget() {
        _budgetLiveData.value = getMonthlyBudget()
    }

    fun setMonthlyBudget(amount: Double) {
        budgetRepository.saveMonthlyBudget(amount)
        _budgetLiveData.value = amount
    }

    fun getMonthlyBudget(): Double {
        return budgetRepository.getMonthlyBudget()
    }

    fun getCurrentMonthExpenses(): Double {
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

    fun isBudgetExceeded(): Boolean {
        val budget = getMonthlyBudget()
        val expenses = getCurrentMonthExpenses()
        return expenses > budget
    }

    fun getBudgetUsagePercentage(): Double {
        val budget = getMonthlyBudget()
        if (budget == 0.0) return 0.0

        val expenses = getCurrentMonthExpenses()
        return (expenses / budget) * 100
    }

    fun getBudgetRemaining(): Double {
        val budget = getMonthlyBudget()
        if (budget == 0.0) return 0.0

        val expenses = getCurrentMonthExpenses()
        return (budget - expenses)
    }
}