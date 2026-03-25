package com.example.InvestEd.repository

import com.example.InvestEd.model.Lesson
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LessonRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val notificationRepository: NotificationRepository = NotificationRepository()
) {
    private fun uid() = auth.currentUser?.uid ?: error("User not logged in")
    private fun userDoc() = db.collection("users").document(uid())

    suspend fun getLessons(): Result<List<Lesson>> {
        return try {
            val userSnap = userDoc().get().await()
            @Suppress("UNCHECKED_CAST")
            val completed = userSnap.get("completedLessons") as? Map<String, Boolean> ?: emptyMap()

            val snapshot = db.collection("modules")
                .orderBy("order")
                .get().await()

            val lessons = snapshot.documents.mapNotNull { doc ->
                Lesson(
                    id          = doc.id,
                    title       = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    content     = doc.getString("content") ?: "",
                    icon        = doc.getString("icon") ?: "📚",
                    duration    = doc.getString("duration") ?: "5 min read",
                    points      = (doc.getLong("points") ?: 10L).toInt(),
                    order       = (doc.getLong("order") ?: 0L).toInt(),
                    color       = doc.getString("color") ?: "mod-navy",
                    completions = (doc.getLong("completions") ?: 0L).toInt()
                )
            }
            Result.success(lessons)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ Now correctly placed at class level, not inside another function
    suspend fun getCompletedLessonsCount(): Int {
        return try {
            val snap      = userDoc().get().await()
            val completed = snap.get("completedLessons") as? Map<*, *> ?: return 0
            completed.values.count { it == true }
        } catch (e: Exception) { 0 }
    }

    suspend fun completeLesson(lessonId: String, points: Int, lessonTitle: String): Result<Unit> {
        return try {
            val userId = uid()

            // 1. Update user document (points and completed map)
            userDoc().update(
                mapOf(
                    "completedLessons.$lessonId" to true,
                    "rewardPoints" to FieldValue.increment(points.toLong())
                )
            ).await()

            // 2. Increment completions count on the module
            db.collection("modules").document(lessonId)
                .update("completions", FieldValue.increment(1)).await()

            // 3. Track who completed it
            db.collection("modules").document(lessonId)
                .collection("completedBy")
                .document(userId)
                .set(mapOf(
                    "completedAt" to FieldValue.serverTimestamp(),
                    "userId" to userId
                )).await()

            // 4. Send notification
            notificationRepository.addNotification(
                "📚 Lesson Completed",
                "You completed \"$lessonTitle\" and earned $points points!",
                "LESSON"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}