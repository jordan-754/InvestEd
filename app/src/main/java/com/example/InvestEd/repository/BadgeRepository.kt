package com.example.InvestEd.repository

import com.example.InvestEd.model.Badge
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BadgeRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun uid() = auth.currentUser?.uid ?: ""
    private fun userDoc() = db.collection("users").document(uid())

    suspend fun getBadges(): Result<List<Badge>> {
        return try {
            if (uid().isEmpty()) return Result.failure(Exception("Not logged in"))

            val userSnap = userDoc().get().await()
            val unlockedIds = (userSnap.get("unlockedBadges") as? List<*>)
                ?.map { it.toString() } ?: emptyList()

            val badgeSnap = db.collection("badges")
                .orderBy("order")
                .get().await()

            val badges = badgeSnap.documents.mapNotNull { doc ->
                Badge(
                    id = doc.id,
                    name = doc.getString("name") ?: return@mapNotNull null,
                    description = doc.getString("description") ?: "",
                    icon = doc.getString("icon") ?: "🏆",
                    points = doc.getLong("points")?.toInt() ?: 0,
                    isUnlocked = unlockedIds.contains(doc.id)
                )
            }

            Result.success(badges)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlockBadge(badgeId: String): Boolean {
        return try {
            if (uid().isEmpty()) return false

            val userSnap = userDoc().get().await()
            val unlockedIds = (userSnap.get("unlockedBadges") as? List<*>)
                ?.map { it.toString() } ?: emptyList()
            if (unlockedIds.contains(badgeId)) return false

            val badgeSnap = db.collection("badges").document(badgeId).get().await()
            val badgeName = badgeSnap.getString("name") ?: "Badge"
            val badgePoints = badgeSnap.getLong("points")?.toInt() ?: 0

            userDoc().update("unlockedBadges", FieldValue.arrayUnion(badgeId)).await()
            userDoc().update("rewardPoints", FieldValue.increment(badgePoints.toLong())).await()

            userDoc().collection("notifications").add(
                mapOf(
                    "title" to "🏆 New Badge Unlocked!",
                    "message" to "You earned the '$badgeName' badge and $badgePoints pts!",
                    "type" to "BADGE",
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()

            true
        } catch (e: Exception) {
            false
        }
    }

    // ✅ Called on app load — checks each badge's trigger and auto-unlocks if condition is met
    suspend fun checkAndUnlockBadges(
        totalSavings: Double,
        totalInvested: Double,
        completedLessonsCount: Int,
        hasReachedGoal: Boolean
    ) {
        try {
            val badgeSnap = db.collection("badges").get().await()
            badgeSnap.documents.forEach { doc ->
                val badgeId = doc.id
                val trigger = doc.getString("trigger") ?: return@forEach
                val shouldUnlock = when (trigger) {
                    // ✅ Existing triggers
                    "first_saving" -> totalSavings > 0
                    "first_invest" -> totalInvested > 0
                    "lessons_5" -> completedLessonsCount >= 5
                    "goal_reached" -> hasReachedGoal

                    // ✅ New savings triggers
                    "savings_1000" -> totalSavings >= 1000
                    "savings_500" -> totalSavings >= 500

                    // ✅ New investment triggers
                    "invest_500" -> totalInvested >= 500
                    "invest_1000" -> totalInvested >= 1000

                    // ✅ New lesson triggers
                    "lessons_1" -> completedLessonsCount >= 1
                    "lessons_all" -> completedLessonsCount >= 10

                    // ✅ New streak/login triggers (handled separately)
                    else -> false
                }
                if (shouldUnlock) unlockBadge(badgeId)
            }
        } catch (e: Exception) {
            // silent fail
        }
    }
}