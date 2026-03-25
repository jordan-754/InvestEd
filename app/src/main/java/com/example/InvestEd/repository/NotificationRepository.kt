package com.example.InvestEd.repository

import com.example.InvestEd.model.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NotificationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun uid() = auth.currentUser?.uid ?: error("User not logged in")
    private fun notifCol() = db.collection("users").document(uid()).collection("notifications")

    suspend fun addNotification(title: String, message: String, type: String): Result<Unit> {
        return try {
            val data = hashMapOf(
                "title" to title,
                "message" to message,
                "type" to type,
                "timestamp" to System.currentTimeMillis()
            )
            notifCol().add(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotifications(): Result<List<Notification>> {
        return try {
            val snap = notifCol()
                .limit(50)
                .get().await()
            val list = snap.documents.mapNotNull { doc ->
                Notification(
                    id = doc.id,
                    title = doc.getString("title") ?: return@mapNotNull null,
                    message = doc.getString("message") ?: "",
                    type = doc.getString("type") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L
                )
            }
            val sorted = list.sortedByDescending { it.timestamp }
            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
