package com.example.InvestEd.model

data class Badge(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val icon: String = "🏆",
    val points: Int = 0,
    val isUnlocked: Boolean = false
)