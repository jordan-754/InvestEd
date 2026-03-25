// repository/InvestmentRepository.kt
package com.example.InvestEd.repository

import com.example.InvestEd.model.Investment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class InvestmentRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun uid() = auth.currentUser?.uid ?: error("User not logged in")
    private fun userDoc() = db.collection("users").document(uid())
    private fun investmentsCol() = userDoc().collection("investments")

    companion object {
        const val DAILY_INTEREST_RATE = 0.001
    }

    suspend fun invest(amount: Double): Result<Investment> {
        return try {
            val date = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                .format(java.util.Date())
            val data = hashMapOf(
                "amount" to amount,
                "date" to date,
                "status" to "approved"
            )
            val ref = investmentsCol().add(data).await()
            Result.success(Investment(id = ref.id, amount = amount, date = date))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitPendingInvestment(amount: Double): Result<Unit> {
        return try {
            val user = auth.currentUser ?: error("User not logged in")
            val userSnap = userDoc().get().await()
            val data = hashMapOf(
                "userId" to user.uid,
                "email" to (user.email ?: ""),
                "userName" to (userSnap.getString("firstName") ?: user.email ?: ""),
                "amount" to amount,
                "type" to "investment",
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("transactions").add(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class LatestCashIn(
        val status: String,
        val amount: Double
    )

    suspend fun getLatestCashIn(): LatestCashIn? {
        return try {
            val snap = db.collection("transactions")
                .whereEqualTo("userId", uid())
                .whereEqualTo("type", "investment")
                .get()
                .await()

            val latest = snap.documents
                .mapNotNull { doc ->
                    val createdAt = doc.getTimestamp("createdAt")
                    val status = doc.getString("status") ?: return@mapNotNull null
                    val amount = doc.getDouble("amount") ?: return@mapNotNull null
                    Triple(createdAt, status, amount)
                }
                .sortedByDescending { (createdAt, _, _) ->
                    createdAt?.toDate()?.time ?: Long.MIN_VALUE
                }
                .firstOrNull()
                ?: return null

            LatestCashIn(status = latest.second, amount = latest.third)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun applyDailyInterest(): Result<Unit> {
        return try {
            val uid = uid()
            val snap = db.collection("transactions")
                .whereEqualTo("userId", uid)
                .whereEqualTo("type", "investment")
                .whereEqualTo("status", "approved")
                .get()
                .await()

            val now = java.util.Date()

            for (doc in snap.documents) {
                val lastApplied = doc.getTimestamp("lastInterestApplied")?.toDate()
                val amount = doc.getDouble("amount") ?: continue
                var totalInterest = doc.getDouble("totalInterest") ?: 0.0

                // Calculate missed days
                val missedDays = if (lastApplied == null) {
                    1
                } else {
                    val diffMs = now.time - lastApplied.time
                    (diffMs / (1000 * 60 * 60 * 24)).toInt()
                }

                if (missedDays <= 0) continue

                // Apply interest for each missed day
                var totalEarned = 0.0
                repeat(missedDays) {
                    val earned = amount * DAILY_INTEREST_RATE
                    totalInterest += earned
                    totalEarned += earned
                }

                val newCurrentValue = amount + totalInterest

                doc.reference.update(
                    mapOf(
                        "totalInterest" to totalInterest,
                        "currentValue" to newCurrentValue,
                        "lastInterestApplied" to FieldValue.serverTimestamp()
                    )
                ).await()

                // Add to budget as SAVINGS
                val date = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                    .format(java.util.Date())
                val budgetData = hashMapOf(
                    "label"  to "Daily Interest${if (missedDays > 1) " ($missedDays days)" else ""}",
                    "amount" to totalEarned,
                    "type"   to "SAVINGS",
                    "date"   to date
                )
                db.collection("users").document(uid)
                    .collection("budget")
                    .add(budgetData)
                    .await()

                // Send notification
                val notifData = hashMapOf(
                    "userId"       to uid,
                    "title"        to "Daily Interest Earned! 💰",
                    "message"      to if (missedDays > 1)
                        "You earned ₱${String.format("%.2f", totalEarned)} interest for $missedDays days on your investment."
                    else
                        "You earned ₱${String.format("%.2f", totalEarned)} daily interest on your investment.",
                    "type"         to "interest",
                    "isRead"       to false,
                    "relatedTxnId" to doc.id,
                    "createdAt"    to FieldValue.serverTimestamp()
                )
                db.collection("notifications")
                    .add(notifData)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTotalCurrentValue(): Double {
        return try {
            val snap = db.collection("transactions")
                .whereEqualTo("userId", uid())
                .whereEqualTo("type", "investment")
                .whereEqualTo("status", "approved")
                .get()
                .await()
            snap.documents.sumOf { it.getDouble("currentValue") ?: 0.0 }
        } catch (e: Exception) {
            0.0
        }
    }

    suspend fun getTotalInterestEarned(): Double {
        return try {
            val snap = db.collection("transactions")
                .whereEqualTo("userId", uid())
                .whereEqualTo("type", "investment")
                .whereEqualTo("status", "approved")
                .get()
                .await()
            snap.documents.sumOf { it.getDouble("totalInterest") ?: 0.0 }
        } catch (e: Exception) {
            0.0
        }
    }

    suspend fun getTotalInvested(): Double {
        return try {
            val snap = db.collection("transactions")
                .whereEqualTo("userId", uid())
                .whereEqualTo("type", "investment")
                .whereEqualTo("status", "approved")
                .get()
                .await()
            snap.documents.sumOf { it.getDouble("amount") ?: 0.0 }
        } catch (e: Exception) {
            0.0
        }
    }
    suspend fun submitWithdrawal(
        amount: Double,
        source: String,
        walletType: String   = "",
        walletNumber: String = "",
        fullName: String     = ""
    ): Result<Unit> {
        return try {
            val user     = auth.currentUser ?: error("User not logged in")
            val userSnap = userDoc().get().await()

            // ✅ Check available balance
            if (source == "investment") {
                val currentValue = getTotalCurrentValue()
                if (amount > currentValue)
                    return Result.failure(Exception("Amount exceeds your investment value of ₱${String.format("%.2f", currentValue)}"))
            } else {
                val budgetSnap = db.collection("users").document(uid())
                    .collection("budget").get().await()
                val savings = budgetSnap.documents
                    .filter { it.getString("type") == "SAVINGS" }
                    .sumOf { it.getDouble("amount") ?: 0.0 } -
                        budgetSnap.documents
                            .filter { it.getString("type") == "SPENDING" }
                            .sumOf { it.getDouble("amount") ?: 0.0 }
                if (amount > savings)
                    return Result.failure(Exception("Amount exceeds your savings of ₱${String.format("%.2f", savings)}"))
            }

            val data = hashMapOf(
                "userId"     to user.uid,
                "email"      to (user.email ?: ""),
                "userName"   to (userSnap.getString("firstName") ?: user.email ?: ""),
                "amount"     to amount,
                "type"       to "withdrawal",
                "source"     to source,
                "status"     to "pending",
                "walletType"   to walletType,    // ✅ new
                "walletNumber" to walletNumber,  // ✅ new
                "fullName"     to fullName,      // ✅ new
                "createdAt"  to FieldValue.serverTimestamp()
            )
            db.collection("transactions").add(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun cleanupDuplicateBudgetEntries(): Result<Unit> {
        return try {
            val budgetCol = db.collection("users").document(uid()).collection("budget")
            val snap = budgetCol.get().await()
            val seenTxIds = mutableSetOf<String>()
            val seenLabelsAndAmounts = mutableSetOf<String>()

            for (doc in snap.documents) {
                val txId = doc.getString("sourceTransactionId")
                val label = doc.getString("label") ?: ""
                val amount = doc.getDouble("amount") ?: 0.0
                val key = "$label-$amount"

                if (txId != null) {
                    if (seenTxIds.contains(txId)) {
                        doc.reference.delete().await()
                        android.util.Log.d("CLEANUP", "Deleted duplicate txId: $txId")
                    } else {
                        seenTxIds.add(txId)
                    }
                } else if (label == "Cash in investment") {
                    if (seenLabelsAndAmounts.contains(key)) {
                        doc.reference.delete().await()
                        android.util.Log.d("CLEANUP", "Deleted duplicate: $key")
                    } else {
                        seenLabelsAndAmounts.add(key)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun cleanupBudgetEntriesWithoutSourceId(): Result<Unit> {
        return try {
            val budgetCol = db.collection("users").document(uid()).collection("budget")
            val snap = budgetCol.get().await()

            for (doc in snap.documents) {
                val label = doc.getString("label") ?: ""
                val sourceId = doc.getString("sourceTransactionId")

                // Delete any "Cash in investment" entry that has no sourceTransactionId
                if (label == "Cash in investment" && sourceId == null) {
                    doc.reference.delete().await()
                    android.util.Log.d("CLEANUP", "Deleted orphan budget entry: ${doc.id}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}