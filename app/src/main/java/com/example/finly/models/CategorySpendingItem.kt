package com.example.finly.models

import java.util.Date

data class CategorySpendingItem(
    val category: String,
    val amount: Double,
    val iconResId: Int,
    val color: Int,
    val percentOfBudget: Double = 0.0,
    val lastUpdated: Date = Date()
)
