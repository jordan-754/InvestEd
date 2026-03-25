// repository/TransactionRepository.kt
package com.example.InvestEd.repository

import com.example.InvestEd.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TransactionRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getTransactionHistory(): Result<List<Transaction>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            // Remove orderBy to avoid requiring a composite index in Firestore
            val snap = db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val list = snap.toObjects(Transaction::class.java)
            
            // Sort by date in Kotlin (client-side)
            val sortedList = list.sortedByDescending { it.createdAt }
            
            Result.success(sortedList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
