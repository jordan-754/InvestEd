// model/Budget.kt
package com.example.InvestEd.model

data class BudgetEntry(
    val id: String,
    val label: String,
    val amount: Double,
    val type: Type,  // SAVINGS or SPENDING
    val date: String
) {
    enum class Type { SAVINGS, SPENDING }
}