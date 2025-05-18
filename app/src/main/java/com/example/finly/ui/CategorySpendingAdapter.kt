package com.example.finly.ui

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finly.R
import com.example.finly.models.CategorySpendingItem
import com.example.finly.repositories.BudgetRepository
import com.example.finly.utils.formatAmount

class CategorySpendingAdapter: ListAdapter<CategorySpendingItem, CategorySpendingAdapter.ViewHolder> (
    object : DiffUtil.ItemCallback<CategorySpendingItem>() {
        override fun areItemsTheSame(
            oldItem: CategorySpendingItem,
            newItem: CategorySpendingItem
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: CategorySpendingItem,
            newItem: CategorySpendingItem
        ): Boolean {
            return oldItem == newItem
        }
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_spending, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.iv_category)
        private val iconContainer: CardView = itemView.findViewById(R.id.icon_container)
        private val nameView: TextView = itemView.findViewById(R.id.tv_category_name)
        private val amountView: TextView = itemView.findViewById(R.id.tv_category_amount)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progress_category)

        fun bind(item: CategorySpendingItem) {
            iconView.setImageResource(item.iconResId)

            val categoryColor = item.color

            iconView.setColorFilter(categoryColor)
            iconContainer.setCardBackgroundColor(ColorUtils.setAlphaComponent(categoryColor, 26))
            amountView.setTextColor(categoryColor)
            progressBar.progressTintList = ColorStateList.valueOf(categoryColor)

            val isNightMode = (itemView.context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            val backgroundAlpha = if (isNightMode) 40 else 20
            iconContainer.setCardBackgroundColor(ColorUtils.setAlphaComponent(categoryColor, backgroundAlpha))

            nameView.text = item.category
            nameView.ellipsize = TextUtils.TruncateAt.END
            nameView.maxLines = 1


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

            amountView.text = "$currencySymbol${item.amount.formatAmount()}"
            amountView.setTextColor(categoryColor)

            val budgetRepository = BudgetRepository(itemView.context)
            val monthlyBudget = budgetRepository.getMonthlyBudget()

            if (monthlyBudget > 0) {
                val percentage = ((item.amount / monthlyBudget) * 100).toInt().coerceAtMost(100)
                progressBar.progress = percentage
            } else {
                progressBar.progress = 100
            }

            progressBar.progressTintList = ColorStateList.valueOf(categoryColor)
        }
    }
}