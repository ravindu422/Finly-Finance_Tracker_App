package com.example.finly.ui

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.finly.R
import com.example.finly.models.Transaction
import com.example.finly.models.TransactionType
import com.example.finly.utils.formatAmount
import java.util.Locale

class TransactionAdapter (
    private val onItemClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.ViewHolder> (
    object: DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
       val view = LayoutInflater.from(parent.context)
           .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.tv_transaction_title)
        private val amountView: TextView = itemView.findViewById(R.id.tv_transaction_amount)
        private val categoryView: TextView = itemView.findViewById(R.id.tv_transaction_category)
        private val dateView: TextView = itemView.findViewById(R.id.tv_transaction_date)

        fun bind(transaction: Transaction) {
            titleView.text = transaction.title

            val preferences = itemView.context.getSharedPreferences(
                "user_prefs", Context.MODE_PRIVATE
            )
            val currencySymbol = when (preferences.getString("currency", "USD")) {
                "USD" -> "$"
                "EUR" -> "€"
                "JPY" -> "¥"
                "CAD" -> "$"
                "LKR" -> "Rs."
                else -> "$"
            }

            val formattedAmount = "${currencySymbol}${transaction.amount.formatAmount()}"
            amountView.text = formattedAmount
            amountView.setTextColor(
                if (transaction.type == TransactionType.INCOME) {
                    ContextCompat.getColor(itemView.context, R.color.income_green)
                } else {
                    ContextCompat.getColor(itemView.context, R.color.expense_red)
                }
            )

            categoryView.text = transaction.category
            dateView.text = SimpleDateFormat("MMM dd, yyyy" , Locale.getDefault())
                .format(transaction.date)

            itemView.setOnClickListener {
                onItemClick(transaction)
            }
        }
    }
}