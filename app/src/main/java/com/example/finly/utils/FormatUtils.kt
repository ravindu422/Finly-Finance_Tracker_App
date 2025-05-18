package com.example.finly.utils

import java.util.Locale

fun Double.formatAmount(): String {
    return String.format(Locale.US,"%.2f", this)
}