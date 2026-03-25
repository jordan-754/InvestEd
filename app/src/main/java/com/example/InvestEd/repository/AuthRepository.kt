package com.example.InvestEd.repository

import com.example.InvestEd.model.User
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class AuthRepository private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: AuthRepository? = null
        fun getInstance(): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository().also { INSTANCE = it }
            }
        }
    }

    private val auth      = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var currentUser: User? = null

    // ─── LOGIN ────────────────────────────────────────────────────────────────
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result       = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception("Login failed"))

            // ✅ Block login if email not verified
            if (!firebaseUser.isEmailVerified) {
                auth.signOut()
                return Result.failure(Exception("Please verify your email before logging in. Check your Gmail inbox."))
            }

            val uid = firebaseUser.uid

            // ✅ Move from pending_users to users if email just verified
            val pendingDoc = firestore.collection("pending_users").document(uid).get().await()
            if (pendingDoc.exists()) {
                val data = pendingDoc.data ?: emptyMap<String, Any>()
                firestore.collection("users").document(uid).set(data).await()
                firestore.collection("pending_users").document(uid).delete().await()
            }

            // ✅ Update lastActive on every login
            firestore.collection("users").document(uid)
                .update("lastActive", com.google.firebase.firestore.FieldValue.serverTimestamp())
                .await()

            val doc  = firestore.collection("users").document(uid).get().await()
            val user = User(
                firstName = doc.getString("firstName") ?: "",
                lastName  = doc.getString("lastName")  ?: "",
                email     = email,
                birthdate = doc.getString("birthdate") ?: "",
                username  = doc.getString("username")  ?: "",
                accountId = doc.getString("accountId") ?: uid
            )
            currentUser = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Login failed"))
        }
    }

    // ─── REGISTER ─────────────────────────────────────────────────────────────
    suspend fun register(
        firstName: String, lastName: String, email: String,
        birthdate: String, school: String, place: String,
        password: String
    ): Result<User> {
        return try {
            val ageError = validateAge(birthdate)
            if (ageError != null) return Result.failure(Exception(ageError))
            if (school.isBlank()) return Result.failure(Exception("Please enter your school"))
            if (place.isBlank())  return Result.failure(Exception("Please enter your place"))

            // ✅ Sign out any previous session
            auth.signOut()

            // ✅ Create Firebase Auth account
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid    = result.user?.uid
                ?: return Result.failure(Exception("Registration failed"))

            // ✅ Send verification email
            result.user?.sendEmailVerification()?.await()

            // ✅ Save to pending_users WHILE still signed in
            // DO NOT sign out before this
            val fullName   = "$firstName $lastName"
            val pendingMap = hashMapOf(
                "email"        to email,
                "firstName"    to firstName,
                "lastName"     to lastName,
                "fullName"     to fullName,
                "username"     to firstName.lowercase(),
                "accountId"    to uid,
                "role"         to "student",
                "status"       to "active",
                "birthdate"    to birthdate,
                "school"       to school,
                "place"        to place,
                "rewardPoints" to 0L,
                "createdAt"    to com.google.firebase.Timestamp.now()
            )

            try {
                firestore.collection("pending_users").document(uid).set(pendingMap).await()
            } catch (firestoreEx: Exception) {
                // ✅ Firestore failed — delete auth account so user can retry
                android.util.Log.d("REGISTER", "Firestore failed: ${firestoreEx.message} — deleting auth account")
                result.user?.delete()?.await()
                auth.signOut()
                return Result.failure(Exception("Registration failed. Please try again."))
            }

            // ✅ Sign out AFTER Firestore write succeeds
            auth.signOut()

            val user = User(
                firstName = firstName,
                lastName  = lastName,
                email     = email,
                birthdate = birthdate,
                username  = firstName.lowercase(),
                accountId = uid
            )
            currentUser = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Registration failed"))
        }
    }

    // ─── VALIDATE AGE (13+ years) ─────────────────────────────────────────────
    private fun validateAge(birthdate: String): String? {
        return try {
            val parts = birthdate.split("/")
            if (parts.size != 3) return "Invalid date format (MM/DD/YYYY)"
            val month = parts[0].toInt()
            val day   = parts[1].toInt()
            val year  = parts[2].toInt()

            val today    = Calendar.getInstance()
            val birthday = Calendar.getInstance().apply {
                set(year, month - 1, day)
            }

            var age = today.get(Calendar.YEAR) - birthday.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthday.get(Calendar.DAY_OF_YEAR)) {
                age--
            }

            if (age < 13) "You must be at least 13 years old to register"
            else null
        } catch (e: Exception) {
            "Invalid birthdate"
        }
    }

    // ─── GOOGLE SIGN IN ───────────────────────────────────────────────────────
    // Just reads info from Google account — NO Firebase Auth call
    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<User> {
        return try {
            val displayName = account.displayName ?: ""
            val parts       = displayName.split(" ")
            val firstName   = parts.firstOrNull() ?: ""
            val lastName    = parts.drop(1).joinToString(" ")
            val email       = account.email ?: ""

            val user = User(
                firstName = firstName,
                lastName  = lastName,
                email     = email,
                birthdate = "",
                username  = firstName.lowercase(),
                accountId = ""
            )
            currentUser = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Google sign-in failed"))
        }
    }

    // ─── IS PROFILE COMPLETE ──────────────────────────────────────────────────
    suspend fun isProfileComplete(): Boolean {
        return try {
            val uid = auth.currentUser?.uid ?: return false
            val doc = firestore.collection("users").document(uid).get().await()
            if (!doc.exists()) return false
            val school    = doc.getString("school")    ?: ""
            val place     = doc.getString("place")     ?: ""
            val birthdate = doc.getString("birthdate") ?: ""
            school.isNotBlank() && place.isNotBlank() && birthdate.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }

    // ─── COMPLETE PROFILE ─────────────────────────────────────────────────────
    suspend fun completeProfile(birthdate: String, school: String, place: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Not logged in"))
            firestore.collection("users").document(uid).update(
                mapOf(
                    "birthdate" to birthdate,
                    "school"    to school,
                    "place"     to place
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to update profile"))
        }
    }

    // ─── OTP / PASSWORD RESET ─────────────────────────────────────────────────
    suspend fun sendOtp(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to send reset email"))
        }
    }

    suspend fun verifyOtp(enteredOtp: String): Result<Unit> {
        return if (enteredOtp.isNotBlank()) Result.success(Unit)
        else Result.failure(Exception("Please enter the code"))
    }

    fun getRegisteredUser(): User? = currentUser

    fun logout() {
        auth.signOut()
        currentUser = null
    }
}