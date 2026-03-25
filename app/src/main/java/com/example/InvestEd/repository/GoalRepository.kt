package com.example.InvestEd.repository

import com.example.InvestEd.model.BudgetEntry
import com.example.InvestEd.model.Goal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoalRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val notificationRepository: NotificationRepository = NotificationRepository(),
    private val budgetRepository: BudgetRepository = BudgetRepository()
) {
    private fun uid() = auth.currentUser?.uid ?: error("User not logged in")
    private fun goalsCol() = db.collection("users").document(uid()).collection("goals")
    private fun userDoc() = db.collection("users").document(uid())

    suspend fun getGoals(): Result<List<Goal>> {
        return try {
            val snapshot = goalsCol().get().await()
            val goals = snapshot.documents.mapNotNull { doc ->
                val isCompleted      = doc.getBoolean("isCompleted") ?: false
                val withdrawalStatus = doc.getString("withdrawalStatus") ?: ""

                // ✅ Hide goals that are fully completed and withdrawal approved
                if (isCompleted && withdrawalStatus == "approved") return@mapNotNull null

                Goal(
                    id            = doc.id,
                    title         = doc.getString("title") ?: return@mapNotNull null,
                    targetAmount  = doc.getDouble("targetAmount") ?: 0.0,
                    currentAmount = doc.getDouble("currentAmount") ?: 0.0,
                    deadline      = doc.getString("deadline") ?: ""
                )
            }
            Result.success(goals)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addGoal(title: String, targetAmount: Double, deadline: String): Result<Goal> {
        return try {
            val data = hashMapOf(
                "title" to title,
                "targetAmount" to targetAmount,
                "currentAmount" to 0.0,
                "deadline" to deadline
            )
            val ref = goalsCol().add(data).await()
            notificationRepository.addNotification(
                "New Goal Added",
                "You set a new goal: \"$title\" — ₱${String.format("%.2f", targetAmount)} by $deadline",
                "GOAL_ADD"
            )
            Result.success(Goal(ref.id, title, targetAmount, 0.0, deadline))
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
            val entries = budgetRepository.getBudget().getOrNull() ?: emptyList()
            val realBalance = entries.filter { it.type == BudgetEntry.Type.SAVINGS }.sumOf { it.amount } -
                    entries.filter { it.type == BudgetEntry.Type.SPENDING }.sumOf { it.amount }

            if (realBalance < addedAmount) return Result.failure(Exception("Insufficient balance"))

            goalsCol().document(goalId).update("currentAmount", newAmount).await()
            budgetRepository.addBudgetEntry(
                label = "Goal: $goalTitle",
                amount = addedAmount,
                type = BudgetEntry.Type.SPENDING
            )
            notificationRepository.addNotification(
                "Goal Progress Updated",
                "You added ₱${String.format("%.2f", addedAmount)} to \"$goalTitle\"",
                "GOAL_AMOUNT"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGoal(goalId: String, goalTitle: String): Result<Unit> {
        return try {
            val goalSnap = goalsCol().document(goalId).get().await()
            val currentAmount = goalSnap.getDouble("currentAmount") ?: 0.0
            goalsCol().document(goalId).delete().await()
            if (currentAmount > 0) {
                budgetRepository.addBudgetEntry(
                    label = "Refund: $goalTitle",
                    amount = currentAmount,
                    type = BudgetEntry.Type.SAVINGS
                )
            }
            notificationRepository.addNotification(
                "Goal Deleted",
                "\"$goalTitle\" was deleted. ₱${String.format("%.2f", currentAmount)} refunded to your balance.",
                "GOAL_DELETE"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeGoal(goalId: String, goalTitle: String): Result<Unit> {
        return try {
            val goalSnap = goalsCol().document(goalId).get().await()
            val currentAmount = goalSnap.getDouble("currentAmount") ?: 0.0
            goalsCol().document(goalId).update(
                mapOf("currentAmount" to 0.0, "isCompleted" to true)
            ).await()
            if (currentAmount > 0) {
                budgetRepository.addBudgetEntry(
                    label = "Goal completed: $goalTitle",
                    amount = currentAmount,
                    type = BudgetEntry.Type.SAVINGS
                )
            }
            notificationRepository.addNotification(
                "Goal Completed",
                "\"$goalTitle\" is completed. ₱${String.format("%.2f", currentAmount)} moved back to your savings.",
                "GOAL_COMPLETE"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyMonthlyInterest(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val budgetCol = db.collection("users").document(userId).collection("budget")
            val now = java.util.Calendar.getInstance()
            val currentMonthKey = "${now.get(java.util.Calendar.YEAR)}-${now.get(java.util.Calendar.MONTH) + 1}"
            val userSnap = userDoc().get().await()
            val lastInterestMonth = userSnap.getString("lastGoalInterestMonth") ?: ""
            if (lastInterestMonth == currentMonthKey) return Result.success(Unit)
            val dayOfMonth = now.get(java.util.Calendar.DAY_OF_MONTH)
            if (dayOfMonth != 1) return Result.success(Unit)

            val goalsSnap = goalsCol().get().await()
            var totalInterest = 0.0
            for (doc in goalsSnap.documents) {
                val currentAmount = doc.getDouble("currentAmount") ?: 0.0
                val isCompleted = doc.getBoolean("isCompleted") ?: false
                if (isCompleted || currentAmount <= 0.0) continue
                totalInterest += currentAmount * 0.03
            }
            if (totalInterest <= 0.0) return Result.success(Unit)

            val date = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())
            budgetCol.add(hashMapOf(
                "label"  to "Monthly Goal Interest (3%)",
                "amount" to totalInterest,
                "type"   to BudgetEntry.Type.SAVINGS.name,
                "date"   to date
            )).await()

            userDoc().update("lastGoalInterestMonth", currentMonthKey).await()
            notificationRepository.addNotification(
                "Goal Interest Earned!",
                "You earned ₱${String.format("%.2f", totalInterest)} (3% monthly interest) from your goals.",
                "GOAL_INTEREST"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ Test version — skips date check, can run anytime
    suspend fun applyMonthlyInterestTest(): Result<Double> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val budgetCol = db.collection("users").document(userId).collection("budget")

            val goalsSnap = goalsCol().get().await()
            var totalInterest = 0.0
            for (doc in goalsSnap.documents) {
                val currentAmount = doc.getDouble("currentAmount") ?: 0.0
                val isCompleted = doc.getBoolean("isCompleted") ?: false
                if (isCompleted || currentAmount <= 0.0) continue
                totalInterest += currentAmount * 0.03
            }
            if (totalInterest <= 0.0) return Result.success(0.0)

            val date = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())
            budgetCol.add(hashMapOf(
                "label"  to "Monthly Goal Interest (3%)",
                "amount" to totalInterest,
                "type"   to BudgetEntry.Type.SAVINGS.name,
                "date"   to date
            )).await()

            notificationRepository.addNotification(
                "Goal Interest Earned!",
                "You earned ₱${String.format("%.2f", totalInterest)} (3% monthly interest) from your goals.",
                "GOAL_INTEREST"
            )
            Result.success(totalInterest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitGoalWithdrawal(
        goalId: String,
        goalTitle: String,
        amount: Double,
        walletType: String,
        walletNumber: String,
        fullName: String
    ): Result<Unit> {
        return try {
            val user = auth.currentUser ?: error("Not logged in")
            val userSnap = userDoc().get().await()
            val userName = userSnap.getString("firstName") ?: user.email ?: ""

            // ✅ Submit withdrawal request to transactions collection
            db.collection("transactions").add(hashMapOf(
                "userId"       to user.uid,
                "email"        to (user.email ?: ""),
                "userName"     to userName,
                "amount"       to amount,
                "type"         to "goal_withdrawal",
                "status"       to "pending",
                "goalId"       to goalId,
                "goalTitle"    to goalTitle,
                "walletType"   to walletType,
                "walletNumber" to walletNumber,
                "fullName"     to fullName,
                "createdAt"    to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )).await()

            // ✅ Mark goal as pending withdrawal
            goalsCol().document(goalId).update(
                mapOf("withdrawalStatus" to "pending")
            ).await()

            notificationRepository.addNotification(
                "Withdrawal Requested",
                "Your withdrawal of ₱${String.format("%,.2f", amount)} from \"$goalTitle\" is pending admin approval.",
                "GOAL_WITHDRAWAL"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}