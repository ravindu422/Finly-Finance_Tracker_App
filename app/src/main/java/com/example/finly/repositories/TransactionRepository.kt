package com.example.finly.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.finly.models.Transaction
import com.example.finly.models.TransactionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TransactionRepository(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(
        "transaction_prefs", Context.MODE_PRIVATE
    )
    private val gson = Gson()

    private val listeners = mutableSetOf<() -> Unit>()

    fun addTransactionsChangeListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeTransactionsChangeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyTransactionsChanged() {
        listeners.forEach { it.invoke() }
    }

    fun saveTransactions(transactions: List<Transaction>) {
        val json = gson.toJson(transactions)
        sharedPreferences.edit().putString("transactions", json).apply()

        notifyTransactionsChanged()
    }

    fun getTransactions(): List<Transaction> {
        val json = sharedPreferences.getString("transactions", null) ?: return emptyList()
        val type = object : TypeToken<List<Transaction>>() {}.type
        return gson.fromJson(json, type)
    }

    fun getTransactionsByType(type: TransactionType): LiveData<List<Transaction>> {
        val allTransactions = getTransactions()
        val filteredTransactions = allTransactions.filter { it.type == type }

        return MutableLiveData<List<Transaction>>().apply {
            value = filteredTransactions
        }
    }

    fun addTransaction(transaction: Transaction) {
        val transactions = getTransactions().toMutableList()
        transactions.add(transaction)
        saveTransactions(transactions)
    }

    fun deleteTransaction(id: String) {
        val transactions = getTransactions().toMutableList()
        transactions.removeAll { it.id == id }
        saveTransactions(transactions)
    }

    fun updateTransaction(updatedTransaction: Transaction) {
        val transactions = getTransactions().toMutableList()
        val index = transactions.indexOfFirst { it.id == updatedTransaction.id }
        if (index != -1) {
            transactions[index] = updatedTransaction
            saveTransactions(transactions)
        }
    }

    private val budgetCheckListeners = mutableSetOf<() -> Unit>()

    fun addBudgetCheckListener(listener: () -> Unit) {
        budgetCheckListeners.add(listener)
    }
    fun removeBudgetCheckListener(listener: () -> Unit) {
        budgetCheckListeners.remove(listener)
    }
    private fun notifyBudgetCheck() {
        budgetCheckListeners.forEach { it.invoke() }
    }

    fun checkBudgetStatus() {
        notifyBudgetCheck()
    }
}
