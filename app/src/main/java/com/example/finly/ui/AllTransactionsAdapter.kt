package com.example.finly.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finly.databinding.ItemTransactionWithDeleteBinding
import com.example.finly.models.Transaction
import com.example.finly.models.TransactionType
import com.example.finly.repositories.UserPreferencesRepository
import java.util.Locale

class AllTransactionsAdapter(
    private val onDeleteClicked: (Transaction) -> Unit,
    private val onEditClicked: (Transaction) -> Unit
) : ListAdapter<Transaction, AllTransactionsAdapter.ViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionWithDeleteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTransactionWithDeleteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            val context = binding.root.context
            val currencySymbol = getCurrencySymbol(context)

            binding.tvTransactionTitle.text = transaction.title
            binding.tvTransactionCategory.text = transaction.category
            binding.tvTransactionDate.text = SimpleDateFormat(
                "dd MMM yyyy", Locale.getDefault()
            ).format(transaction.date)
            val amount = transaction.amount
            binding.tvTransactionAmount.text = "$currencySymbol${amount.formatAmount()}"


            val textColor = when (transaction.type) {
                TransactionType.INCOME -> Color.parseColor("#4CAF50")
                TransactionType.EXPENSE -> Color.parseColor("#F44336")
            }
            binding.tvTransactionAmount.setTextColor(textColor)

            binding.root.setOnClickListener {
                onEditClicked(transaction)
            }
            binding.btnDelete.setOnClickListener {
                onDeleteClicked(transaction)
            }
        }

        private fun isNightMode(context: Context): Boolean {
            return (context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

        private fun getCurrencySymbol(context: Context): String {
            val preferencesRepository = UserPreferencesRepository(context)
            return when (preferencesRepository.getCurrency()) {
                "USD" -> "$"
                "EUR" -> "€"
                "JPY" -> "¥"
                "CAD" -> "$"
                "LKR" -> "Rs."
                else -> "$"
            }
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}

fun Double.formatAmount(): String {
    return String.format(Locale.getDefault(), "%,.0f", this)
}