package com.example.InvestEd.model

data class Goal(
    val id: String = "",
    val title: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val deadline: String = ""
) {
    val progressPercent: Int
        get() = if (targetAmount > 0) ((currentAmount / targetAmount) * 100).toInt() else 0

    val progressPercentFloat: Float
        get() = if (targetAmount > 0) ((currentAmount / targetAmount) * 100).toFloat() else 0f

    // ✅ Display like "₱20.00 / ₱30,000.00"
    val savedVsTarget: String
        get() = "₱${String.format("%,.2f", currentAmount)} / ₱${String.format("%,.2f", targetAmount)}"
}