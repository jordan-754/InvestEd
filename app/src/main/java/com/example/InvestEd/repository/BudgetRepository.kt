package com.example.InvestEd.repository

import com.example.InvestEd.model.BudgetEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class BudgetRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val notificationRepository: NotificationRepository = NotificationRepository()
) {
    private fun uid() = auth.currentUser?.uid ?: error("User not logged in")
    private fun budgetCol() = db.collection("users").document(uid()).collection("budget")

    suspend fun getBudget(): Result<List<BudgetEntry>> {
        return try {
            val snapshot = budgetCol().get().await()
            val entries = snapshot.documents.mapNotNull { doc ->
                BudgetEntry(
                    id     = doc.id,
                    label  = doc.getString("label") ?: return@mapNotNull null,
                    amount = doc.getDouble("amount") ?: 0.0,
                    type   = BudgetEntry.Type.valueOf(doc.getString("type") ?: "SAVINGS"),
                    date   = doc.getString("date") ?: ""
                )
            }
            Result.success(entries.sortedByDescending { it.date })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addBudgetEntry(label: String, amount: Double, type: BudgetEntry.Type): Result<BudgetEntry> {
        return try {
            val date = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())
            val data = hashMapOf(
                "label"  to label,
                "amount" to amount,
                "type"   to type.name,
                "date"   to date
            )
            val ref = budgetCol().add(data).await()

            notificationRepository.addNotification(
                " Budget Entry Added",
                "Added ₱${String.format("%.2f", amount)} — $label (${type.name})",
                "BUDGET"
            )
            Result.success(BudgetEntry(ref.id, label, amount, type, date))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Balance = Savings - Spending ──────────────────────────────────────────
    suspend fun getTotalAmount(): Double {
        return try {
            val snap = budgetCol().get().await()
            val docs = snap.documents
            val totalSavings  = docs
                .filter { it.getString("type") == "SAVINGS" }
                .sumOf { it.getDouble("amount") ?: 0.0 }
            val totalSpending = docs
                .filter { it.getString("type") == "SPENDING" }
                .sumOf { it.getDouble("amount") ?: 0.0 }
            totalSavings - totalSpending
        } catch (e: Exception) {
            0.0
        }
    }

    suspend fun getSavingsPercent(): Int {
        return try {
            val snap = budgetCol().get().await()
            val docs          = snap.documents
            val totalSavings  = docs
                .filter { it.getString("type") == "SAVINGS" }
                .sumOf { it.getDouble("amount") ?: 0.0 }
            val totalSpending = docs
                .filter { it.getString("type") == "SPENDING" }
                .sumOf { it.getDouble("amount") ?: 0.0 }
            val total = totalSavings + totalSpending
            if (total == 0.0) return 0
            ((totalSavings / total) * 100).toInt()
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getSpendingPercent(): Int {
        return try {
            val snap = budgetCol().get().await()
            val docs          = snap.documents
            val totalSavings  = docs
                .filter { it.getString("type") == "SAVINGS" }
                .sumOf { it.getDouble("amount") ?: 0.0 }
            val totalSpending = docs
                .filter { it.getString("type") == "SPENDING" }
                .sumOf { it.getDouble("amount") ?: 0.0 }
            val total = totalSavings + totalSpending
            if (total == 0.0) return 0
            ((totalSpending / total) * 100).toInt()
        } catch (e: Exception) {
            0
        }
    }
}