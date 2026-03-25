package com.example.InvestEd.viewmodel

import androidx.lifecycle.*
import androidx.lifecycle.viewModelScope
import com.example.InvestEd.repository.AuthRepository
import com.example.InvestEd.repository.BadgeRepository
import com.example.InvestEd.repository.MainRepository
import com.example.InvestEd.repository.LessonRepository
import com.example.InvestEd.repository.GoalRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class HomeViewModel(
    private val repository: MainRepository = MainRepository(),
    private val authRepository: AuthRepository = AuthRepository.getInstance(),
    private val badgeRepository: BadgeRepository = BadgeRepository(),
    private val lessonRepository: LessonRepository = LessonRepository()
) : ViewModel() {

    data class HomeData(
        val totalSavings: Double,
        val totalInvested: Double,
        val rewardPoints: Int,
        val userName: String

    )


    sealed class HomeState {
        object Loading : HomeState()
        data class Success(val data: HomeData) : HomeState()
        data class Error(val message: String) : HomeState()
    }

    private val _state = MutableLiveData<HomeState>(HomeState.Loading)
    val state: LiveData<HomeState> = _state

    init { loadHomeData() }
    init {
        loadHomeData()
        applyMonthlyInterestIfDue()  // ✅ add this
    }
    fun loadHomeData() {
        _state.value = HomeState.Loading
        viewModelScope.launch {
            val result = repository.getHomeData()
            if (result.isSuccess) {
                val triple   = result.getOrThrow()
                val balance  = triple.first
                val invested = triple.second
                val points   = triple.third

                val user = authRepository.getRegisteredUser()
                var name = user?.firstName

                if (name.isNullOrBlank()) {
                    try {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        if (uid != null) {
                            val doc = FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .get()
                                .await()
                            name = doc.getString("firstName")
                                ?: doc.getString("fullName")
                                        ?: "User"
                        } else {
                            name = "User"
                        }
                    } catch (e: Exception) {
                        name = "User"
                    }
                }

                _state.value = HomeState.Success(
                    HomeData(
                        totalSavings  = balance,
                        totalInvested = invested,
                        rewardPoints  = points.toInt(),
                        userName      = name ?: "User"
                    )
                )

                checkBadges(totalSavings = balance, totalInvested = invested)

            } else {
                _state.value = HomeState.Error("Failed to load data")
            }
        }
    }

    private suspend fun checkBadges(totalSavings: Double, totalInvested: Double) {
        try {
            val completedLessons = lessonRepository.getCompletedLessonsCount()  // ← changed
            val hasReachedGoal   = repository.hasReachedGoal()

            badgeRepository.checkAndUnlockBadges(
                totalSavings          = totalSavings,
                totalInvested         = totalInvested,
                completedLessonsCount = completedLessons,
                hasReachedGoal        = hasReachedGoal
            )
        } catch (e: Exception) {
            // silent fail
        }
    }

    private fun applyMonthlyInterestIfDue() {
        viewModelScope.launch {
            GoalRepository().applyMonthlyInterest()
        }
    }
}