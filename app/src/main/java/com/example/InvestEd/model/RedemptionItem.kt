package com.example.InvestEd.model

data class RedemptionItem(
    val id: String = "",
    val cost: Long = 0,
    val pesosReward: Double = 0.0,
    val status: String = "pending",
    val requestedAt: com.google.firebase.Timestamp? = null
)
