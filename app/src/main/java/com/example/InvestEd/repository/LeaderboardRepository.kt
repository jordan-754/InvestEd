package com.example.InvestEd.repository

import com.example.InvestEd.model.LeaderboardEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LeaderboardRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    fun currentUserId() = auth.currentUser?.uid ?: ""

    suspend fun getTopUsers(): List<LeaderboardEntry> {
        return try {
            val usersSnap = db.collection("users").get().await()
            val entries = mutableListOf<LeaderboardEntry>()

            for (userDoc in usersSnap.documents) {
                val userId = userDoc.id
                val fullName = userDoc.getString("fullName") ?: ""
                val firstName = userDoc.getString("firstName") ?: ""
                val lastName = userDoc.getString("lastName") ?: ""
                val name = when {
                    fullName.isNotBlank() -> fullName
                    firstName.isNotBlank() -> "$firstName $lastName".trim()
                    else -> userDoc.getString("email")?.substringBefore("@") ?: "User"
                }
                val points = userDoc.getLong("rewardPoints")?.toDouble() ?: 0.0

                entries.add(
                    LeaderboardEntry(
                        userId = userId,
                        name = name,
                        score = points,
                        totalInvested = 0.0,
                        lessonsCompleted = 0,
                        goalsCompleted = 0
                    )
                )
            }

            entries.sortedByDescending { it.score }
                .take(10)
                .mapIndexed { index, entry -> entry.copy(rank = index + 1) }

        } catch (e: Exception) {
            emptyList()
        }
    }
}