package com.example.InvestEd.repository

import com.example.InvestEd.model.BudgetEntry
import com.example.InvestEd.model.Goal
import com.example.InvestEd.model.RedemptionItem
import com.example.InvestEd.model.RewardItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.firestore.FieldValue

class MainRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val budgetRepository: BudgetRepository = BudgetRepository(),
    private val investmentRepository: InvestmentRepository = InvestmentRepository()
) {
    private fun uid() = auth.currentUser?.uid ?: error("User not logged in")
    private fun userDoc() = db.collection("users").document(uid())
    private fun goalsCol() = userDoc().collection("goals")

    private suspend fun ensureUserDoc() {
        val snap = userDoc().get().await()
        if (!snap.exists()) {
            userDoc().set(
                mapOf(
                    "rewardPoints"     to 0L,
                    "lastLessonDate"   to "",
                    "completedLessons" to emptyMap<String, Boolean>(),
                    "unlockedBadges"   to emptyList<String>()
                ),
                SetOptions.merge()
            ).await()
        }
    }
    

    /**
     * Syncs approved investment transactions into the user's budget as SAVINGS entries.
     *
     * When the admin approves a "cash in" (transaction in `transactions` with
     * type = "investment" and status = "approved"), we mirror that amount into
     * the user's `budget` subcollection so it increases currentBalance and savings.
     *
     * Each transaction is only synced once, tracked via `sourceTransactionId`.
     */
    private suspend fun syncApprovedInvestmentsToBudget() {
        try {
            val userId = auth.currentUser?.uid ?: return

            val userSnap = userDoc().get().await()
            @Suppress("UNCHECKED_CAST")
            val syncedIds = (userSnap.get("syncedInvestmentIds") as? List<String>)
                ?.toMutableSet() ?: mutableSetOf()

            val txSnap = db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "approved")
                .whereEqualTo("type", "investment")
                .get().await()

            val budgetCol = userDoc().collection("budget")
            val newlySynced = mutableListOf<String>()

            for (doc in txSnap.documents) {
                val txId   = doc.id
                val amount = doc.getDouble("amount") ?: 0.0
                if (amount <= 0.0) continue

                // ✅ Skip if already in syncedInvestmentIds
                if (syncedIds.contains(txId)) continue

                // ✅ Skip if already in budget collection
                val existing = budgetCol
                    .whereEqualTo("sourceTransactionId", txId)
                    .limit(1)
                    .get().await()
                if (!existing.isEmpty) {
                    newlySynced.add(txId)
                    continue
                }

                // ✅ Add only once
                val date = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())
                budgetCol.add(hashMapOf(
                    "label"               to "Cash in investment",
                    "amount"              to amount,
                    "type"                to BudgetEntry.Type.SAVINGS.name,
                    "date"                to date,
                    "sourceTransactionId" to txId
                )).await()

                newlySynced.add(txId)
            }

            if (newlySynced.isNotEmpty()) {
                userDoc().update(
                    "syncedInvestmentIds",
                    FieldValue.arrayUnion(*newlySynced.toTypedArray())
                ).await()
            }

        } catch (e: Exception) {
            // Fail silently
        }
    }

    suspend fun getRewardPoints(): Result<Long> {
        return try {
            val snap = userDoc().get().await()
            Result.success(snap.getLong("rewardPoints") ?: 0L)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── GOALS ────────────────────────────────────────────────────────────────
    suspend fun getGoals(): Result<List<Goal>> {
        return try {
            val snap = goalsCol().get().await()
            val goals = snap.documents.mapNotNull { doc ->
                Goal(
                    id            = doc.id,
                    title         = doc.getString("title")        ?: return@mapNotNull null,
                    targetAmount  = doc.getDouble("targetAmount") ?: 0.0,
                    currentAmount = doc.getDouble("currentAmount") ?: 0.0,
                    deadline      = doc.getString("deadline")     ?: ""
                )
            }
            Result.success(goals)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addGoal(title: String, targetAmount: Double, deadline: String): Result<Unit> {
        return try {
            ensureUserDoc()
            goalsCol().add(
                mapOf(
                    "title"         to title,
                    "targetAmount"  to targetAmount,
                    "currentAmount" to 0.0,
                    "deadline"      to deadline
                )
            ).await()

            // Notification (Emoji Removed)
            userDoc().collection("notifications").add(
                mapOf(
                    "title"     to "New Goal Added",
                    "message"   to "You created a new goal: \"$title\"",
                    "type"      to "GOAL_ADD",
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGoalCurrentAmount(
        goalId: String,
        newAmount: Double,
        goalTitle: String,
        addedAmount: Double
    ): Result<Unit> {
        return try {
            goalsCol().document(goalId).update("currentAmount", newAmount).await()

            budgetRepository.addBudgetEntry(
                label  = "Goal: $goalTitle",
                amount = addedAmount,
                type   = BudgetEntry.Type.SPENDING
            )

            // Notification (Emoji Removed)
            userDoc().collection("notifications").add(
                mapOf(
                    "title"     to "Goal Progress Updated",
                    "message"   to "Added pesos ${String.format("%.2f", addedAmount)} to \"$goalTitle\"",
                    "type"      to "GOAL_AMOUNT",
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGoal(goalId: String, goalTitle: String): Result<Unit> {
        return try {
            goalsCol().document(goalId).delete().await()

            // Notification (Emoji Removed)
            userDoc().collection("notifications").add(
                mapOf(
                    "title"     to "Goal Deleted",
                    "message"   to "You deleted the goal: \"$goalTitle\"",
                    "type"      to "GOAL_DELETE",
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasReachedGoal(): Boolean {
        return try {
            val goals = goalsCol().get().await()
            goals.documents.any { doc ->
                val target  = doc.getDouble("targetAmount") ?: 0.0
                val current = doc.getDouble("currentAmount") ?: 0.0
                target > 0 && current >= target
            }
        } catch (e: Exception) { false }
    }

    // ─── REWARDS ──────────────────────────────────────────────────────────────
    suspend fun getRewardItems(): Result<List<RewardItem>> {
        return try {
            // Load conversion rate from global settings
            val rateSnap = db.collection("settings").document("conversionRate").get().await()
            val conversionPoints = rateSnap.getLong("points") ?: 1000000L
            val conversionPesos  = rateSnap.getDouble("pesos") ?: 100.0

            val snap = db.collection("rewards")
                .orderBy("cost", Query.Direction.ASCENDING)
                .get().await()

            val list = snap.documents.map { doc ->
                val cost = doc.getLong("cost") ?: 0L
                RewardItem(
                    id              = doc.id,
                    cost            = cost,
                    color           = doc.getString("color") ?: "#1B3A6B",
                    pesosEquivalent = (cost.toDouble() / conversionPoints) * conversionPesos
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyRedemptions(): Result<List<RedemptionItem>> {
        return try {
            val snap = db.collection("redemptions")
                .whereEqualTo("studentId", uid())
                .orderBy("requestedAt", Query.Direction.DESCENDING)
                .get().await()

            val list = snap.documents.map { doc ->
                RedemptionItem(
                    id          = doc.id,
                    cost        = doc.getLong("cost") ?: 0L,
                    pesosReward = doc.getDouble("pesosReward") ?: 0.0,
                    status      = doc.getString("status") ?: "pending",
                    requestedAt = doc.getTimestamp("requestedAt")
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestRedemption(pointsCost: Long, pesosReward: Double): Result<Unit> {
        return try {
            val user = auth.currentUser ?: error("Not logged in")

            db.runTransaction { transaction ->
                val snapshot = transaction.get(userDoc())
                val currentPoints = snapshot.getLong("rewardPoints") ?: 0L

                if (currentPoints < pointsCost) {
                    throw Exception("Not enough points")
                }

                // 1. Deduct points
                transaction.update(userDoc(), "rewardPoints", currentPoints - pointsCost)

                // 2. Create redemption record (for history)
                val name = snapshot.getString("fullName") ?: user.email ?: "User"
                val redemptionRef = db.collection("redemptions").document()
                transaction.set(redemptionRef, mapOf(
                    "studentId"   to user.uid,
                    "studentName" to name,
                    "cost"        to pointsCost,
                    "pesosReward" to pesosReward,
                    "status"      to "approved", // ← auto approved
                    "requestedAt" to Timestamp.now(),
                    "resolvedAt"  to Timestamp.now()
                ))

                // 3. Add notification
                val notificationRef = userDoc().collection("notifications").document()
                transaction.set(notificationRef, mapOf(
                    "title"     to "Points Exchanged!",
                    "message"   to "You exchanged ${String.format("%,d", pointsCost)} pts for ₱${String.format("%.2f", pesosReward)}.",
                    "type"      to "EXCHANGE",
                    "timestamp" to System.currentTimeMillis()
                ))
            }.await()

            // 4. Add pesos directly to budget as SAVINGS
            budgetRepository.addBudgetEntry(
                label  = "🎁 Points Exchanged",
                amount = pesosReward,
                type   = BudgetEntry.Type.SAVINGS
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun processApprovedWithdrawal(txnId: String, amount: Double, source: String, userId: String): Result<Unit> {
        return try {
            if (source == "investment") {
                // Deduct from investment currentValue
                val snap = db.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("type", "investment")
                    .whereEqualTo("status", "approved")
                    .get().await()

                var remaining = amount
                for (doc in snap.documents) {
                    if (remaining <= 0) break
                    val currentValue = doc.getDouble("currentValue") ?: 0.0
                    val deduct = minOf(remaining, currentValue)
                    doc.reference.update("currentValue", currentValue - deduct).await()
                    remaining -= deduct
                }
            } else {
                // Deduct from savings via budget entry
                db.collection("users").document(userId)
                    .collection("budget")
                    .add(mapOf(
                        "label"  to "Withdrawal",
                        "amount" to amount,
                        "type"   to "SPENDING",
                        "date"   to java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date())
                    )).await()
            }

            // Send notification to user
            db.collection("notifications").add(mapOf(
                "userId"    to userId,
                "title"     to "Withdrawal Approved! 💸",
                "message"   to "Your withdrawal of ₱${String.format("%.2f", amount)} has been approved.",
                "type"      to "withdrawal",
                "isRead"    to false,
                "createdAt" to FieldValue.serverTimestamp()
            )).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun cleanupDuplicateBudgetEntries(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val budgetCol = userDoc().collection("budget")

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
                        // Duplicate — delete it
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
    // ─── HOME DATA ────────────────────────────────────────────────────────────
    suspend fun getHomeData(): Result<Triple<Double, Double, Long>> {
        return try {
            ensureUserDoc()

            val budgetEntries = budgetRepository.getBudget().getOrNull() ?: emptyList()
            val totalSavings = budgetEntries
                .filter { it.type == BudgetEntry.Type.SAVINGS }
                .sumOf { it.amount } -
                    budgetEntries
                        .filter { it.type == BudgetEntry.Type.SPENDING }
                        .sumOf { it.amount }

            val goalsSnap     = goalsCol().get().await()
            val totalInvested = goalsSnap.documents.sumOf { it.getDouble("currentAmount") ?: 0.0 }

            val userSnap     = userDoc().get().await()
            val rewardPoints = userSnap.getLong("rewardPoints") ?: 0L

            Result.success(Triple(totalSavings, totalInvested, rewardPoints))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}