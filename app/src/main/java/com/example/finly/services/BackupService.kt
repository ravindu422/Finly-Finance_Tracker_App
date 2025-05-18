package com.example.finly.services

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.finly.models.Transaction
import com.example.finly.repositories.TransactionRepository
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class BackupService(private val context: Context) {
    private val transactionRepository = TransactionRepository(context)
    private val gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()

    fun exportTransactions(): File {
        val transactions = transactionRepository.getTransactions()
        val json = gson.toJson(transactions)

        val fileName = "Finly_backup_${System.currentTimeMillis()}.json"
        val file = File(context.getExternalFilesDir(null), fileName)

        try {
            FileOutputStream(file).use { fos ->
                fos.write(json.toByteArray())
            }
            return file
        } catch (e: Exception) {
            throw IOException("Failed to create backup file", e)
        }
    }

    fun importTransactions(uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val json = inputStream?.bufferedReader().use { it?.readText() } ?: return false

            val type = object : TypeToken<List<Transaction>>() {}.type
            val importedTransactions: List<Transaction> = gson.fromJson(json, type)

            for (transaction in importedTransactions) {
                val existingTransactions = transactionRepository.getTransactions()
                val existingTransaction = existingTransactions.find { it.id == transaction.id }

                if (existingTransaction != null) {
                    transactionRepository.updateTransaction(transaction)
                } else {
                    transactionRepository.addTransaction(transaction)
                }
            }

            true
        } catch (e: Exception) {
            Log.e("BackupService", "Import error", e)
            false
        }
    }
}
