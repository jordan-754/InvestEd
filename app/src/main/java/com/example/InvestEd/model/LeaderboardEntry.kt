package com.example.InvestEd.model

data class LeaderboardEntry(
    val userId: String = "",
    val name: String = "",
    val score: Double = 0.0,
    val totalInvested: Double = 0.0,
    val lessonsCompleted: Int = 0,
    val goalsCompleted: Int = 0,
    val rank: Int = 0
)