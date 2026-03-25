package com.example.InvestEd.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.InvestEd.model.LeaderboardEntry
import com.example.InvestEd.repository.LeaderboardRepository
import kotlinx.coroutines.launch

class LeaderboardViewModel(
    private val repository: LeaderboardRepository = LeaderboardRepository()
) : ViewModel() {

    sealed class LeaderboardState {
        object Loading : LeaderboardState()
        object Empty : LeaderboardState()
        data class Success(val entries: List<LeaderboardEntry>, val currentUserId: String?) : LeaderboardState()
        data class Error(val message: String) : LeaderboardState()
    }

    private val _state = MutableLiveData<LeaderboardState>(LeaderboardState.Loading)
    val state: LiveData<LeaderboardState> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = LeaderboardState.Loading
            try {
                val entries = repository.getTopUsers()
                if (entries.isEmpty()) {
                    _state.value = LeaderboardState.Empty
                } else {
                    _state.value = LeaderboardState.Success(entries, repository.currentUserId())
                }
            } catch (e: Exception) {
                _state.value = LeaderboardState.Error(e.message ?: "Failed to load leaderboard")
            }
        }
    }
}
