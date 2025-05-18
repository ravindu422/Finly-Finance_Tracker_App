package com.example.finly.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class TransactionType : Parcelable {
    INCOME, EXPENSE
}