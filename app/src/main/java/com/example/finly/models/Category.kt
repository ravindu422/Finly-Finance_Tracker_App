package com.example.finly.models

import java.util.UUID

data class Category(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: Int,
    val color: Int
)
