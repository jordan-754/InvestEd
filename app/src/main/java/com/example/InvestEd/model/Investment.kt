package com.example.InvestEd.model

data class Investment(
    val id: String = "",
    val userId: String = "",       // Firebase Auth UID
    val amount: Double = 0.0,
    val date: String = "",
    val timestamp: Long = 0L       // for sorting
)