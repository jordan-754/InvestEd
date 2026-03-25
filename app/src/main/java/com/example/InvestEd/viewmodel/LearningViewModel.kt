// viewmodel/LearningViewModel.kt
package com.example.InvestEd.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.InvestEd.model.Lesson
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

class LearningViewModel : ViewModel() {

    sealed class LearningState {
        object Loading : LearningState()
        data class Success(val lessons: List<Lesson>) : LearningState()
        object Empty : LearningState()
        data class Error(val message: String) : LearningState()
    }

    sealed class CompletionState {
        object Idle : CompletionState()
        data class Success(val points: Int, val lessonTitle: String) : CompletionState()
        data class Error(val message: String) : CompletionState()
    }

    sealed class ActionState {
        object Idle : ActionState()
        data class Success(val points: Int) : ActionState()
        object AlreadyDone : ActionState()
        data class Error(val message: String) : ActionState()
    }

    private val _learningState = MutableLiveData<LearningState>(LearningState.Loading)
    val learningState: LiveData<LearningState> = _learningState

    private val _completedLessons = MutableLiveData<Set<String>>(emptySet())
    val completedLessons: LiveData<Set<String>> = _completedLessons

    private val _completionState = MutableLiveData<CompletionState>(CompletionState.Idle)
    val completionState: LiveData<CompletionState> = _completionState

    private val _actionState = MutableLiveData<ActionState>(ActionState.Idle)
    val actionState: LiveData<ActionState> = _actionState

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        checkDailyResetThenLoad()
    }

    // ─── Daily Reset ──────────────────────────────────────────────────────────

    private fun checkDailyResetThenLoad() {
        val userId = auth.currentUser?.uid ?: run { loadLessons(); return }
        val userRef = db.collection("users").document(userId)
        val today   = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        userRef.get().addOnSuccessListener { snap ->
            val lastLessonDate = snap.getString("lastLessonDate") ?: ""

            if (lastLessonDate != today) {
                userRef.set(
                    mapOf(
                        "lastLessonDate"   to today,
                        "completedLessons" to mapOf<String, Boolean>()
                    ),
                    SetOptions.merge()
                ).addOnCompleteListener {
                    _completedLessons.value = emptySet()
                    loadLessons()
                }
            } else {
                @Suppress("UNCHECKED_CAST")
                val completed = (snap.get("completedLessons") as? Map<String, Boolean>)
                    ?.filterValues { it }
                    ?.keys
                    ?: emptySet()
                _completedLessons.value = completed
                loadLessons()
            }
        }.addOnFailureListener {
            loadLessons()
        }
    }

    // ─── Load Lessons ─────────────────────────────────────────────────────────

    private fun loadLessons() {
        _learningState.value = LearningState.Loading

        db.collection("modules")
            .orderBy("order")
            .get()
            .addOnSuccessListener { result ->
                val lessons = result.documents.mapNotNull { doc ->
                    try {
                        Lesson(
                            id          = doc.id,
                            title       = doc.getString("title")       ?: "",
                            description = doc.getString("description") ?: "",
                            content     = doc.getString("content")     ?: "",
                            icon        = doc.getString("icon")        ?: "📚",
                            duration    = doc.getString("duration")    ?: "5 min read",
                            points      = (doc.getLong("points")       ?: 10L).toInt(),
                            order       = (doc.getLong("order")        ?: 0L).toInt(),
                            color       = doc.getString("color")       ?: "mod-navy",
                            completions = (doc.getLong("completions")  ?: 0L).toInt()
                        )
                    } catch (e: Exception) { null }
                }
                _learningState.value = if (lessons.isEmpty()) LearningState.Empty
                else LearningState.Success(lessons)
            }
            .addOnFailureListener { e ->
                _learningState.value = LearningState.Error(e.message ?: "Failed to load lessons")
            }
    }

    // ─── Complete a Lesson ────────────────────────────────────────────────────

    fun completeLesson(lessonId: String, points: Int, title: String) {
        // ✅ Guard 1: in-memory check (fast, prevents UI double-tap)
        val current = _completedLessons.value ?: emptySet()
        if (current.contains(lessonId)) {
            _actionState.value = ActionState.AlreadyDone
            return
        }

        val userId = auth.currentUser?.uid ?: return

        // ✅ Guard 2: Firestore check before ANY points are awarded
        // Reads the user's completedLessons map from Firestore to verify
        // the lesson wasn't already completed in a previous session or
        // by another ViewModel instance — only then awards points.
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { snap ->
                @Suppress("UNCHECKED_CAST")
                val firestoreCompleted =
                    (snap.get("completedLessons") as? Map<String, Boolean>)
                        ?.filterValues { it }
                        ?.keys
                        ?: emptySet()

                if (firestoreCompleted.contains(lessonId)) {
                    // Already done in Firestore — sync local state and bail
                    _completedLessons.value = firestoreCompleted
                    _actionState.value = ActionState.AlreadyDone
                    return@addOnSuccessListener
                }

                // ── Safe to award — lesson not yet completed ──────────────
                _completedLessons.value = current + lessonId

                // 1) Increment completions count on module
                db.collection("modules").document(lessonId)
                    .update("completions", FieldValue.increment(1))

                // 2) Save to completedBy sub-collection
                db.collection("modules").document(lessonId)
                    .collection("completedBy").document(userId)
                    .set(mapOf(
                        "completedAt" to FieldValue.serverTimestamp(),
                        "userId"      to userId
                    ))

                // 3) Save to user's completedLessons map
                db.collection("users").document(userId)
                    .set(
                        mapOf("completedLessons" to mapOf(lessonId to true)),
                        SetOptions.merge()
                    )

                // 4) Award lesson points — only once, only here
                db.collection("users").document(userId)
                    .update("rewardPoints", FieldValue.increment(points.toLong()))

                // 5) Notification
                db.collection("users").document(userId)
                    .collection("notifications")
                    .add(mapOf(
                        "title"     to "Lesson points earned",
                        "message"   to "You earned $points pts for completing \"$title\" in the Learning Hub.",
                        "type"      to "LESSON_POINTS",
                        "timestamp" to System.currentTimeMillis()
                    ))

                _actionState.value     = ActionState.Success(points)
                _completionState.value = CompletionState.Success(points, title)
            }
            .addOnFailureListener { e ->
                _actionState.value = ActionState.Error(e.message ?: "Could not verify completion")
            }
    }

    fun resetActionState()     { _actionState.value     = ActionState.Idle }
    fun resetCompletionState() { _completionState.value = CompletionState.Idle }
}